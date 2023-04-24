# CleanCode Webcrawler

WebCrawler, welcher von Webseiten Headlines sowie alle Links ausliest. Diese Links können bei Bedarf ebenso ausgelesen
werden.
Alle Daten werden in eine zuvor angegebene Markdown (.md) Datei gespeichert und in ihrer korrekten (Headline-/Such-)
Tiefe visuell dargestellt

## Features

- Argumente werden über Konsole übergeben
- Headlines werden gecrawled und in eine beliebige Sprache übersetzt
- Links der gecrawlten Webseiten werden ausgelesen
- Relative Links werden korrekt erkannt und ausgelesen
- Links zu anderen Webseiten/Subseiten werden ausgelesen und ebenso rekursiv gecrawled
- Nicht funktionsfähige Links werden markiert
- Tiefe der gecrawlten Headlines und Links wird eruiert
- Links und Headlines werden in ihrem korrekten Format in einer zuvor angegebenen Markdown-Datei visuell dargestellt

## Getting Started

### Dependencies

- **Java JDK Version:** 11
- **Module SDK:** oenjdk-17
- **Maven:** 3.5.1 oder höher
- **Verwendeter Code-Editor:** IntelliJ IDEA

### Installation

- Da eine [**Translation-API**](https://rapidapi.com/dickyagustin/api/text-translator2) verwendet wird, muss vor der
  Ausführung ein Translation-API Key als **Systemumgebungsvariable** festgelegt werden
    - **API-Name:** ``RAPIDAPI_API_KEY``
    - **API-Key** Muss über die API-Seite erstellt werden (Login reicht hier aus)
    - **Hinzufügung zu Windows:** Systemumgebungsvariablen bearbeiten -> Umgebungsvariablen -> Benutzervariablen. Nach der Hinzufügung muss IntelliJ neugestartet werden.
- Die **Main**-Klasse dient als Einstiegspunkt für den Crawler. Nach Ausführung wird die Person aufgefordert, die URL,
  Tiefe des Crawlens, Sprache, sowie das Markdown-File anzugeben, in welches geschrieben werden soll.
- Die ausgelesenen Daten können dann in der **Konsole** oder im **Markdown-File** gefunden werden.

### Testausführung

- Mittels integrierter IntelliJ **Maven Test-Funktion** oder mittels **mvn test**
- Der Code wird durch [Sonarcloud](https://sonarcloud.io/project/overview?id=uni-aau_cleancode-webcrawler) bei jedem
  Commit in den Master-Branch analysiert und ein Coverage-Report erstellt.
- Inkludierte Testarten
    - Coverage Report mittels **Jacoco**
    - Mutation Testing via **PIT**


