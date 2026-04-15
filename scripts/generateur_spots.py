import cv2
import numpy as np
from ultralytics import YOLO
import json

# --- CONFIGURATION ---
MODEL_PATH = 'best3.pt'  # Le fichier téléchargé de Colab
IMAGE_TEST = 'parking_photo.jpg'      # Ta photo de parking
MATRIX_PATH = 'perspective_matrix.npy' # Généré par plan_parking.py
OUTPUT_JSON = 'parking_layout.json'    # Le fichier pour Spring Boot

def generer_parking_intelligent():
    print("🚀 Initialisation de l'analyse automatique...")

    # 1. Charger les ressources
    model = YOLO(MODEL_PATH)
    try:
        matrix = np.load(MATRIX_PATH)
    except FileNotFoundError:
        print("❌ Erreur : perspective_matrix.npy introuvable. Lance plan_parking.py d'abord !")
        return

    img = cv2.imread(IMAGE_TEST)
    if img is None:
        print(f"❌ Erreur : Impossible de lire {IMAGE_TEST}")
        return

    # 2. Détection par l'IA
    # On utilise imgsz=640 car c'est ainsi que l'IA a été entraînée sur Colab
    results = model.predict(source=img, conf=0.5, imgsz=640)

    spots_data = []

    # 3. Traitement des résultats
    for r in results:
        for i, box in enumerate(r.boxes):
            # Coordonnées du rectangle détecté
            x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
            cx, cy = (x1 + x2) / 2, (y1 + y2) / 2 # Centre de la place

            # Transformation de perspective (pour avoir des x/y plats)
            pt = np.array([[[cx, cy]]], dtype=np.float32)
            transformed = cv2.perspectiveTransform(pt, matrix)
            x_final, y_final = transformed[0][0]

            # Récupération du label (0 ou 1 selon le dataset)
            label_id = int(box.cls[0])
            label_name = model.names[label_id] # "Free" ou "Busy"

            # Traduction pour ton DTO Spring Boot
            statut = "DISPONIBLE" if "free" in label_name.lower() else "OCCUPE"

            spots_data.append({
                "id": str(i + 1),
                "nom": f"Spot {i + 1}",
                "statut": statut,
                "x": round(float(x_final), 2),
                "y": round(float(y_final), 2),
                "description": f"Détecté par IA ({label_name})"
            })

    # À ajouter juste après la boucle for
    annotated_frame = results[0].plot()
    cv2.imshow("Verification IA", annotated_frame)
    cv2.waitKey(0)

    # 4. Sauvegarde JSON
    with open(OUTPUT_JSON, 'w') as f:
        json.dump(spots_data, f, indent=4)

    print(f"✅ Terminé ! {len(spots_data)} places créées dans {OUTPUT_JSON}")

if __name__ == "__main__":
    generer_parking_intelligent()