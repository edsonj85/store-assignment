# Data generation
The script in here can be used to generate a lot of sample data. It requires node and npm to run

# Installation
```shell
npm install
```

# Execution
```shell
node ./generateData.js > ../src/main/resources/db/changelog/data-load.sql
```

`src/main/resources/db/changelog/data.sql` is the small, fixed dataset used by
the default changelog (`bootRun`, everyday local dev) — do not overwrite it
with generated output. `data-load.sql` is the realistic-volume dataset, applied
only when the `load` Spring profile is active — used both to load-test the app
locally (`SPRING_PROFILES_ACTIVE=load ./gradlew bootRun`) and by the
Testcontainers-backed integration tests. Regenerate that one instead.

# Notes
If you change the liquibase migration, you'll need to hack the liquibase changelog. Or drop and recreate your database