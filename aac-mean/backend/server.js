const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const axios = require('axios');

const app = express();
app.use(cors());
app.use(express.json());

// MongoDB connection
const mongoUri =
  'mongodb://aacuser:SNHU1234@localhost:27017/aac_shelter_outcomes?authSource=admin';

mongoose
  .connect(mongoUri)
  .then(() => console.log('Mongo connected'))
  .catch((err) => console.error('Mongo connection error:', err));

// Generic schema
const animalSchema = new mongoose.Schema({}, { collection: 'ACC', strict: false });
const Animal = mongoose.model('Animal', animalSchema);

// Filter helper
function buildRescueQuery(rescueType) {
  if (rescueType === 'water_rescue') {
    return {
      animal_type: 'Dog',
      breed: { $in: ['Labrador Retriever', 'Newfoundland'] },
      age_upon_outcome_in_weeks: { $gte: 26, $lt: 156 },
      sex_upon_outcome: 'Intact Female',
    };
  }

  if (rescueType === 'mountain_wilderness_rescue') {
    return {
      animal_type: 'Dog',
      breed: {
        $in: [
          'German Shepherd',
          'Alaskan Malamute',
          'Old English Sheepdog',
          'Siberian Husky',
          'Rottweiler',
        ],
      },
      age_upon_outcome_in_weeks: { $gte: 26, $lt: 156 },
      sex_upon_outcome: 'Intact Male',
    };
  }

  if (rescueType === 'disaster_individual_tracking') {
    return {
      animal_type: 'Dog',
      breed: {
        $in: [
          'Doberman Pinscher',
          'German Shepherd',
          'Golden Retriever',
          'Bloodhound',
          'Rottweiler',
        ],
      },
      age_upon_outcome_in_weeks: { $gte: 20, $lt: 300 },
      sex_upon_outcome: 'Intact Male',
    };
  }

  // Default: return all
  return {};
}

// API route: Animals
app.get('/api/animals', async (req, res) => {
  try {
    const rescueType = req.query.rescueType || 'All';
    const query = buildRescueQuery(rescueType);

    const animals = await Animal.find(query).lean();
    res.json(animals);
  } catch (err) {
    console.error('Error fetching animals:', err);
    res.status(500).json({ error: 'Server error' });
  }
});

// ---------- Breed pie data (Top N or All) ----------
//   Top 10 + Other (default): /api/breeds/pie?mode=top&topN=10
//   All breeds:              /api/breeds/pie?mode=all
app.get('/api/breeds/pie', async (req, res) => {
  try {
    const mode = String(req.query.mode || 'top').toLowerCase(); // 'top' | 'all'
    const topN = Number(req.query.topN || 10);

    const basePipeline = [
      { $match: { breed: { $exists: true, $ne: null, $ne: '' } } },
      { $group: { _id: '$breed', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
    ];

    // Return ALL breeds (can be a lot)
    if (mode === 'all') {
      const data = await Animal.aggregate(basePipeline);
      return res.json(data.map((x) => ({ breed: x._id, count: x.count })));
    }

    // Return TOP N + "Other"
    const pipeline = [...basePipeline];

    pipeline.push({
      $facet: {
        top: [{ $limit: topN }],
        rest: [{ $skip: topN }],
      },
    });

    pipeline.push({
      $project: {
        top: 1,
        otherCount: { $ifNull: [{ $sum: '$rest.count' }, 0] },
      },
    });

    pipeline.push({
      $project: {
        data: {
          $concatArrays: ['$top', [{ _id: 'Other', count: '$otherCount' }]],
        },
      },
    });

    const result = await Animal.aggregate(pipeline);
    const data = result?.[0]?.data || [];

    return res.json(
      data
        .filter((x) => x.count > 0)
        .map((x) => ({ breed: x._id, count: x.count }))
    );
  } catch (err) {
    console.error('Error fetching breed pie data:', err);
    res.status(500).json({ error: 'Server error' });
  }
});

// ML: Adoption likelihood prediction
// Endpoint: POST /predict/adoption
const ML_BASE_URL = process.env.ML_BASE_URL || 'http://127.0.0.1:8000';

app.post('/api/predict/adoption', async (req, res) => {
  try {
    const mlResponse = await axios.post(`${ML_BASE_URL}/predict/adoption`, req.body, {
      timeout: 5000,
    });

    return res.json(mlResponse.data);
  } catch (err) {
    console.error('ML service error:', err?.message || err);
    return res.status(502).json({
      error: 'Predictive analytics service unavailable',
      detail: err?.message || String(err),
    });
  }
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`API listening on http://localhost:${PORT}`));
