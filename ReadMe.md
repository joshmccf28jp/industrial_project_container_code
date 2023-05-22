# ConnectorCommon

Contains FileConnenctor war artefact

These are deployed as artifacts to AWS CodeArtifact.

## Installation to local repository

```bash
mvn clean install
```
## Run local jetty server with FileConnector deployed

```bash
mvn jetty:run
```

## Deploying to CodeArtifact

This is handled via pipeline (more details required)