import cv2
import numpy as np # <-- Ajouté pour le traitement d'image
from ultralytics import YOLO
import easyocr
import requests
import time
import re

# --- CONFIGURATION ---
model_yolo = YOLO('best.pt')
reader = easyocr.Reader(['ar', 'en'])
BACKEND_URL = "http://localhost:8080/api/reservations/entree-ia"

def formater_matricule_tn(ocr_results):
    # 1. On ramasse TOUS les chiffres sans exception, triés de gauche à droite
    morceaux = []
    for (bbox, text, prob) in ocr_results:
        # On ne garde que les chiffres et on ignore les confusions avec 'تونس'
        val = "".join(filter(str.isdigit, text))
        if val:
            x_min = bbox[0][0]
            morceaux.append({'x': x_min, 'val': val})

    # Trier par position X
    morceaux.sort(key=lambda m: m['x'])
    chaine_complete = "".join([m['val'] for m in morceaux])

    # 2. Nettoyage des erreurs de lecture communes (doublons ou reflets)
    # Si la chaîne est trop longue (ex: 1333 5133 au lieu de 133 5133),
    # c'est souvent un chiffre lu deux fois.
    if len(chaine_complete) > 7:
         # On ne garde que les 7 derniers chiffres (le max standard)
         chaine_complete = chaine_complete[-7:]

    # 3. Séparation "Standard TN"
    # Le numéro à droite fait TOUJOURS 4 chiffres en Tunisie (sauf vieilles plaques)
    # La série à gauche fait le reste (2 ou 3 chiffres)
    if len(chaine_complete) >= 5:
        numero = chaine_complete[-4:] # Les 4 derniers
        serie = chaine_complete[:-4]  # Tout ce qui reste à gauche
        return f"{serie} تونس {numero}"

    return f"تونس {chaine_complete}" if chaine_complete else "تونس"

def envoyer_au_backend(matricule):
    payload = {
        "matricule": matricule,
        "voitureMarque": "Smart Scanner TN",
        "spontane": True,
        "statusAction": "EN_COURS"
    }
    try:
        response = requests.post(BACKEND_URL, json=payload, timeout=3)
        print(f"🚀 Envoyé : {matricule}")
    except:
        print("❌ Erreur de connexion Backend")

def demarrer_scanner():
    cap = cv2.VideoCapture(0)
    print("📸 Scanner prêt. Appuie sur 'S' pour capturer.")

    while True:
        ret, frame = cap.read()
        if not ret: break

        results = model_yolo(frame, conf=0.6, verbose=False)

        for r in results:
            for box in r.boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)

                if cv2.waitKey(1) & 0xFF == ord('s'):
                    # --- ÉTAPE DE PRÉTRAITEMENT POUR "ENTRAINER" LA VUE ---
                    # 1. Zoom sur la plaque
                    plate_crop = frame[max(0, y1-5):y2+5, max(0, x1-5):x2+5]

                    # 2. Mise en Gris
                    gray = cv2.cvtColor(plate_crop, cv2.COLOR_BGR2GRAY)

                    # 3. Agrandissement (FX=2) : Rend les chiffres plus gros pour l'IA
                    gray = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)

                    # 4. Filtre de contraste (Thresholding) : Rend l'image NOIR et BLANC PUR
                    # Cela élimine les reflets gris sur la plaque
                    _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

                    print("🔍 Analyse haute précision...")
                    ocr_res = reader.readtext(thresh)

                    final_txt = formater_matricule_tn(ocr_res)
                    print(f"✅ Résultat nettoyé : {final_txt}")
                    envoyer_au_backend(final_txt)

        cv2.imshow('SmartPark - IA Precision', frame)
        if cv2.waitKey(1) & 0xFF == ord('q'): break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    demarrer_scanner()