# Lazycord Test-Protokoll

## System-Voraussetzungen
- Docker & Docker Compose installiert
- Mindestens 4GB RAM verfügbar
- Ports frei: 3000, 8080, 8081, 5432, 6379

## Start
```bash
cd /data/.openclaw/workspace/Lazycord
./start-local.sh
```

---

## Test-Features

### 1. 🔐 Authentifizierung
**Test-URL:** http://localhost:3000/login

**Testfälle:**
- [ ] Login-Seite lädt
- [ ] Login mit `admin/admin123`
- [ ] Login mit `user/user123`
- [ ] Registrierung neuer Benutzer
- [ ] Logout-Funktion
- [ ] Fehlermeldung bei falschem Passwort

**Screenshot:** Login-Seite mit erfolgreichem Login

---

### 2. 💬 Chat-Funktion
**Test-URL:** http://localhost:3000 (nach Login)

**Testfälle:**
- [ ] Channel-Liste wird angezeigt
- [ ] Nachricht senden (Text)
- [ ] Nachricht empfangen (zweiter Browser/User)
- [ ] Channel wechseln
- [ ] Nachrichten-History laden

**Screenshot:** Chat mit gesendeten Nachrichten

---

### 3. 📋 Missionen
**Test-URL:** http://localhost:3000/missions

**Testfälle:**
- [ ] Missionen-Seite lädt
- [ ] Tabs: Available/In Progress/Completed
- [ ] Fortschrittsbalke anzeigen
- [ ] Belohnung einfordern (wenn verfügbar)

**Screenshot:** Missionen mit Fortschritt

---

### 4. 🏪 Shop
**Test-URL:** http://localhost:3000/shop

**Testfälle:**
- [ ] Shop lädt
- [ ] Items nach Kategorie filtern
- [ ] Item kaufen (mit Punkten)
- [ ] Inventar anzeigen
- [ ] Item ausrüsten

**Screenshot:** Shop mit gekauftem Item

---

### 5. 👤 Benutzerprofil
**Test:** In Chat-Seite unten links

**Testfälle:**
- [ ] Benutzername anzeigen
- [ ] Punkte/XP anzeigen
- [ ] Level anzeigen
- [ ] Rank anzeigen

**Screenshot:** Profil-Bereich

---

### 6. 🔧 Admin-Features (Keycloak)
**Test-URL:** http://localhost:8081

**Testfälle:**
- [ ] Keycloak Admin Console lädt
- [ ] Login mit `admin/admin`
- [ ] Benutzer-Liste anzeigen
- [ ] Realm-Einstellungen sichtbar

**Screenshot:** Keycloak Admin Console

---

### 7. 🔌 Backend API
**Test-URL:** http://localhost:8080/actuator/health

**Testfälle:**
- [ ] Health-Endpoint erreichbar
- [ ] API-Response zeigt "UP"

**Screenshot:** API Health Response

---

## Screenshots erstellen

### Windows (mit Docker Desktop)
1. Docker Desktop starten
2. `./start-local.sh` in Git Bash/PowerShell ausführen
3. Browser öffnen: http://localhost:3000
4. **Snipping Tool** oder **Win+Shift+S** für Screenshots
5. Screenshots in `screenshots/` Ordner speichern

### macOS
1. Docker Desktop starten
2. Terminal: `./start-local.sh`
3. Browser: http://localhost:3000
4. **Cmd+Shift+4** für Screenshots
5. Screenshots in `screenshots/` Ordner verschieben

### Linux
1. Docker starten: `sudo systemctl start docker`
2. Terminal: `./start-local.sh`
3. Browser: http://localhost:3000
4. Screenshot-Tool (z.B. `gnome-screenshot`)

---

## Erwartete Ergebnisse

| Feature | Status | Screenshot |
|---------|--------|------------|
| Login | ⬜ | ⬜ |
| Chat | ⬜ | ⬜ |
| Missionen | ⬜ | ⬜ |
| Shop | ⬜ | ⬜ |
| Profil | ⬜ | ⬜ |
| Keycloak | ⬜ | ⬜ |
| API Health | ⬜ | ⬜ |

---

## Fehlerbehebung

### Frontend lädt nicht
```bash
docker-compose logs frontend
```

### Backend Fehler
```bash
docker-compose logs backend
```

### Datenbank prüfen
```bash
docker-compose exec postgres psql -U lazycord -d lazycord -c "\dt"
```

### Alles neustarten
```bash
docker-compose down -v
./start-local.sh
```

---

## Abschluss

Nach erfolgreichem Test:
1. Screenshots in `screenshots/` Ordner sammeln
2. Checkliste abhaken
3. Bei Fehlern Logs prüfen
