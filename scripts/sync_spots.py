import requests
import json

# --- CONFIGURATION ---
API_URL = "http://localhost:8080/api/spots"
PARKING_ID = "65f1a2b3c4d5e6f7g8h9i0j1" # Remplace par l'ID réel de ton parking dans MongoDB
JSON_PATH = 'parking_layout.json'

def synchroniser():
    try:
        with open(JSON_PATH, 'r') as f:
            spots = json.load(f)
    except FileNotFoundError:
        print("❌ Erreur : parking_layout.json introuvable. Lance l'IA d'abord !")
        return

    print(f"🔄 Suppression des anciens spots pour le parking {PARKING_ID}...")
    # Optionnel : Tu peux appeler un endpoint DELETE ici si tu veux faire place nette

    print(f"📤 Injection de {len(spots)} nouveaux spots...")

    for spot in spots:
        # Structure exacte de ton SpotDTO
        payload = {
            "nom": spot['nom'],
            "description": spot['description'],
            "statut": spot['statut'],
            "x": spot['x'],
            "y": spot['y'],
            "parking": {"id": PARKING_ID} # Structure imbriquée comme ton DTO
        }

        try:
            res = requests.post(API_URL, json=payload)
            if res.status_code in [200, 201]:
                print(f"✅ {spot['nom']} créé à la position ({spot['x']}, {spot['y']})")
            else:
                print(f"⚠️ Erreur sur {spot['nom']} : {res.text}")
        except Exception as e:
            print(f"❌ Connexion au backend échouée : {e}")

if __name__ == "__main__":
    synchroniser()