import cv2
import numpy as np

# Liste pour stocker les 4 points
points = []

def click_event(event, x, y, flags, params):
    global img
    if event == cv2.EVENT_LBUTTONDOWN:
        # Ajouter le point
        points.append([x, y])

        # Dessiner un cercle et le numéro du clic sur l'image
        cv2.circle(img, (x, y), 5, (0, 255, 0), -1)
        cv2.putText(img, str(len(points)), (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        cv2.imshow("Configuration Parking", img)

        # Dès qu'on a 4 points, on traite et on quitte
        if len(points) == 4:
            sauvegarder_matrice_auto()

def sauvegarder_matrice_auto():
    # Dimensions voulues pour le plan final (Vue de dessus)
    width, height = 800, 600

    # Points de départ (tes clics) et points d'arrivée (rectangle parfait)
    pts_depart = np.float32(points)
    pts_arrivee = np.float32([[0, 0], [width, 0], [0, height], [width, height]])

    # Calcul de la matrice
    matrix = cv2.getPerspectiveTransform(pts_depart, pts_arrivee)

    # Sauvegarde silencieuse
    np.save("perspective_matrix.npy", matrix)

    # Petit feedback visuel rapide avant de fermer
    result = cv2.warpPerspective(img_copy, matrix, (width, height))
    cv2.imshow("Apercu - Fermeture automatique...", result)
    print("✅ Matrice sauvegardée avec succès !")
    cv2.waitKey(1500) # Attend 1.5 seconde pour que tu puisses voir si c'est noir ou pas
    cv2.destroyAllWindows()
    exit() # Ferme le script proprement

# --- MAIN ---
path_image = "parking_photo.jpg"
img = cv2.imread(path_image)

if img is None:
    print(f"❌ Erreur : Impossible de trouver '{path_image}'")
else:
    img_copy = img.copy()
    cv2.imshow("Configuration Parking", img)
    print("🖱️ Cliquez sur les 4 coins (Haut-Gauche, Haut-Droite, Bas-Gauche, Bas-Droite)")
    cv2.setMouseCallback("Configuration Parking", click_event)
    cv2.waitKey(0)