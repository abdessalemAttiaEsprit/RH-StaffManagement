# main.py
import os

from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
# Option sécurisée pour trouver le chemin du fichier
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
model_path = os.path.join(BASE_DIR, "smartpark_full_package_model (1).pkl")
app = FastAPI()
model = joblib.load(model_path)

class PredictionInput(BaseModel):
    role_id: int
    years_experience: int
    cv_score_ai: float
    contract_type_id: int

@app.post("/predict-salary")
def predict(data: PredictionInput):
    # Préparation des données pour le modèle
    input_df = pd.DataFrame([[data.role_id, data.years_experience, data.cv_score_ai, data.contract_type_id]], 
                             columns=['role_id', 'years_experience', 'cv_score_ai', 'contract_type_id'])
    
    prediction = model.predict(input_df)[0]
    
    return {
        "salary_brut": round(prediction[0], 2),
        "overtime_rate": round(prediction[1], 2),
        "total_benefits": round(prediction[2], 2)
    }