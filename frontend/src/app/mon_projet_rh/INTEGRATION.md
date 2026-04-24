# Intégration `mon_projet` → SmartPark (frontend)

## Ce qui a été ajouté
Le code Angular provenant de `C:\Users\USER\Desktop\mon_projet\frontend\src\app` a été copié dans:

- `src/app/mon_projet_rh/`

Aucun fichier existant de SmartPark n’a été modifié.

## Base URL backend (API)
Dans `src/app/mon_projet_rh/service/*`, les URLs ne sont plus figées sur `http://localhost:8082`.

- Base par défaut: `http://localhost:8080`
- Override possible via localStorage:

Dans la console du navigateur:

```js
localStorage.setItem('smartpark_api_base_url', 'http://localhost:8080');
// ou par exemple
localStorage.setItem('smartpark_api_base_url', 'http://localhost:8081');
```

## Brancher les routes (optionnel)
Les routes prêtes à être intégrées sont dans:

- `src/app/mon_projet_rh/mon-projet-rh.routes.ts` (préfixées sous `/rh`)

Pour rendre ces pages accessibles dans SmartPark, il faut *ajouter* ces routes au tableau `routes` principal de SmartPark (ceci implique une modification de `src/app/app.routes.ts`).

Si vous décidez d’autoriser cette micro-modification, dites-moi et je vous propose le patch minimal.
