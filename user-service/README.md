# recipe-service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Local Google sign-in setup

From `C:\Recipe app`, copy the environment example and generate an RSA key pair:

```powershell
if (-not (Test-Path .\user-service\user-service\.env)) {
  Copy-Item .\user-service\user-service\.env.example .\user-service\user-service\.env
}
if (-not (Test-Path .\user-service\user-service\keys\privateKey.pem)) {
  openssl genpkey -algorithm RSA -out .\user-service\user-service\keys\privateKey.pem -pkeyopt rsa_keygen_bits:2048
}
if (-not (Test-Path .\user-service\user-service\keys\publicKey.pem)) {
  openssl pkey -in .\user-service\user-service\keys\privateKey.pem -pubout -out .\user-service\user-service\keys\publicKey.pem
}
```

Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in the ignored `.env` file.
For the Google OAuth web client, register
`http://localhost:8081/api/v1/auth/callback` as an authorized redirect URI.

Start the backend stack:

```powershell
Set-Location 'C:\Recipe app\docker-compose'
docker compose --env-file ..\user-service\user-service\.env up
```

The Compose stack mounts the signing/verification key pair read-only at the
configured container paths. The app JWT has issuer
`https://recipe-app.com/issuer`, a `userId` claim, and both the `role` and
SmallRye `groups` role claims. OAuth credentials and generated private keys
must remain local and must not be committed.

## Production deployment

The GitHub Actions workflow deploys on pushes to `main` that change `user-service/**`
or the workflow itself. Configure the `AZURE_CREDENTIALS`, `DB_USER`, `DB_PASSWORD`,
`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `LOGIN_SUCCESS_URI` repository
secrets before deploying. `LOGIN_SUCCESS_URI` must be the public frontend callback,
not a localhost URL. Register
`https://<user-service-host>/api/v1/auth/callback` as an authorized redirect URI
for the production Google OAuth client.

Configure the Container App to mount its Azure secret volume at `/mnt/secrets`,
with the private and public key secrets available as `jwt-private-key` and
`jwt-public-key`. The workflow supplies these mounted paths to Quarkus; it does
not put key contents in the image. Production schema management is set to
`validate`, so create/update the PostgreSQL schema before deploying the app.
The workflow's database URL must point to the production `user_db` and require
TLS (`sslmode=require`).

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/recipe-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)


[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
