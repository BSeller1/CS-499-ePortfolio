from pymongo import MongoClient
import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score


# MongoDB connection 
MONGO_URI = "mongodb://aacuser:SNHU1234@localhost:27017/aac_shelter_outcomes?authSource=admin"
DB_NAME = "aac_shelter_outcomes"
COLLECTION_NAME = "ACC"

client = MongoClient(MONGO_URI)
collection = client[DB_NAME][COLLECTION_NAME]

# Load data
docs = list(collection.find({
    "outcome_type": {"$ne": None},
    "animal_type": {"$ne": None},
    "sex_upon_outcome": {"$ne": None},
    "breed": {"$ne": None},
    "age_upon_outcome_in_weeks": {"$ne": None},
    "datetime": {"$ne": None}
}))

df = pd.DataFrame(docs)

if df.empty:
    raise RuntimeError("No data returned from MongoDB. Check DB/collection names.")

# Feature engineering
df["is_adopted"] = (df["outcome_type"] == "Adoption").astype(int)
df["primary_breed"] = df["breed"].str.split("/").str[0].str.strip()
df["outcome_month"] = pd.to_datetime(df["datetime"], errors="coerce").dt.month
df["age_weeks"] = pd.to_numeric(df["age_upon_outcome_in_weeks"], errors="coerce")

df = df.dropna(subset=["primary_breed", "outcome_month", "age_weeks"])

# Features / label
X = df[
    ["animal_type", "sex_upon_outcome", "primary_breed", "outcome_month", "age_weeks"]
]
y = df["is_adopted"]

# Preprocessing
categorical = ["animal_type", "sex_upon_outcome", "primary_breed"]
numeric = ["outcome_month", "age_weeks"]

preprocessor = ColumnTransformer(
    transformers=[
        ("cat", OneHotEncoder(handle_unknown="ignore"), categorical),
        ("num", "passthrough", numeric)
    ]
)

# Model
model = Pipeline(
    steps=[
        ("preprocessor", preprocessor),
        ("classifier", LogisticRegression(max_iter=1000))
    ]
)

# Train / test split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

model.fit(X_train, y_train)

# Evaluate
accuracy = accuracy_score(y_test, model.predict(X_test))
print(f"Adoption Model Accuracy: {accuracy:.2%}")

# Save model
joblib.dump(model, "adoption_model.pkl")
print("Model saved as adoption_model.pkl")
