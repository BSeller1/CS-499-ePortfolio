from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import os
import pandas as pd

app = FastAPI(title="AAC Predictive Analytics Service")

MODEL_PATH = os.getenv("ADOPTION_MODEL_PATH", "adoption_model.pkl")

model = None
if os.path.exists(MODEL_PATH):
    model = joblib.load(MODEL_PATH)

FEATURE_COLS = ["animal_type", "sex_upon_outcome", "primary_breed", "age_weeks", "outcome_month"]

class AdoptionPredictRequest(BaseModel):
    animal_type: str
    sex_upon_outcome: str
    primary_breed: str
    age_weeks: float
    outcome_month: int

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}

@app.post("/predict/adoption")
def predict_adoption(req: AdoptionPredictRequest):
    if model is None:
        return {"adoption_probability": 0.5, "note": "Model not loaded; returning default probability."}

    # Build DataFrame with fixed column order
    X = pd.DataFrame([{
        "animal_type": req.animal_type,
        "sex_upon_outcome": req.sex_upon_outcome,
        "primary_breed": req.primary_breed,
        "age_weeks": float(req.age_weeks),
        "outcome_month": int(req.outcome_month),
    }], columns=FEATURE_COLS)

    try:
        proba = float(model.predict_proba(X)[0][1])
        return {"adoption_probability": proba}
    except Exception as e:
        # Return the error
        raise HTTPException(status_code=500, detail=str(e))
