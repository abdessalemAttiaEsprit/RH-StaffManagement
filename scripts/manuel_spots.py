import cv2
import numpy as np
import json

# Configuration
MATRIX_PATH = 'perspective_matrix.npy'
IMAGE_TEST = 'parking_photo.jpg'
spots_manuels = []

def click_spot(event, x, y, flags, params):
    if event == cv2.EVENT_LBUTTONDOWN:
        # On projette le clic sur la vue plate
        pt = np.array([[[x, y]]], dtype=np.float32)
        transformed = cv2.perspectiveTransform(pt, matrix)
        coord = transformed[0][0]

        spots_manuels.append({
            "id": len(spots_manuels) + 1,
            "x_canvas": round(float(coord[0]), 2),
            "y_canvas": round(float(coord[1]), 2),
            "occupe": False
        })

        cv2.circle(img, (x, y), 5, (255, 0, 0), -1)
        cv2.imshow("Cliquez sur chaque place vide", img)
        print(f"Place {len(spots_manuels)} ajoutée.")

# Chargement
matrix = np.load(MATRIX_PATH)
img = cv2.imread(IMAGE_TEST)
cv2.imshow("Cliquez sur chaque place vide", img)
cv2.setMouseCallback("Cliquez sur chaque place vide", click_spot)

print("🖱️ Cliquez au centre de chaque place de parking. Appuyez sur 's' pour sauvegarder et quitter.")

while True:
    key = cv2.waitKey(1) & 0xFF
    if key == ord('s'):
        with open('parking_layout.json', 'w') as f:
            json.dump(spots_manuels, f, indent=4)
        print("✅ Fichier 'parking_layout.json' créé avec succès !")
        break
cv2.destroyAllWindows()