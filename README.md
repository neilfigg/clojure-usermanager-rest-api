<a id="org659cd36"></a>

# Tenfren Example UserManager REST API in Clojure

<a id="org6cb11aa"></a>

## Overview

<img src="./doc/images/tenfren.png" width=130 align="right"/>

The goal of this project is to provide an example [Clojure](https://clojure.org/) REST API for common User Management services such as:

-   Authentication
-   Authorization
-   Registration
-   Login
-   Password management
-   CRUD APIs for Users and Admins.

[Why Clojure?](https://clojure.org/about/rationale). I've never met a language I didn't like, but Clojure is the only language I've ever loved.

This code is from an app I had called Tenfren, so that name is all through the code.


<a id="org8c32fe0"></a>

## Requirements

-   [Clojure 1.10.x](https://clojure.org/guides/getting_started)
-   JDK 11.x
-   PostgreSQL - optional see the [PostgreSQL](#org6a5cf19) section for running PostgreSQL in a Docker container.
-   Docker - optional can run the API from a REPL or as an executable jar. See the [REST API](#orgd567a23) section for running the REST API in a Docker container.


<a id="orgff7ae69"></a>

## Libraries

See `deps.edn` for the full list of dependencies and aliases.

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<tbody>
<tr>
<td class="org-left">Web</td>
<td class="org-left"><a href="https://www.eclipse.org/jetty/">jetty</a></td>
<td class="org-left">HTTP server</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/ring-clojure/ring">ring-clojure/ring</a></td>
<td class="org-left">Clojure web applications library inspired by Python's WSGI and Ruby's Rack.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/ring-clojure/ring">ring-clojure/ring-jetty-adapter</a></td>
<td class="org-left">Ring adapter that uses the Jetty webserver.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/metosin/reitit">metosin/reitit</a></td>
<td class="org-left">Data-driven router for Clojure(Script).</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/metosin/malli">metosin/malli</a></td>
<td class="org-left">Data-driven Schemas for Clojure/Script used for HTTP requests/responses.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/metosin/muuntaja">metosin/muuntaja</a></td>
<td class="org-left">Clojure library for fast http format negotiation, encoding and decoding.</td>
</tr>


<tr>
<td class="org-left">State management</td>
<td class="org-left"><a href="https://github.com/weavejester/integrant">weavejester/integrant</a></td>
<td class="org-left">Clojure(Script) micro-framework for building applications with data-driven architecture.</td>
</tr>


<tr>
<td class="org-left">Security</td>
<td class="org-left"><a href="https://github.com/funcool/buddy-auth">funcool/buddy-auth</a></td>
<td class="org-left">JWT (Json Web Token), authentication, authorization, and password hashers.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/suvash/one-time">suvash/one-time</a></td>
<td class="org-left">Clojure library for generating one time passwords (HOTP &amp; TOTP) as per RFC 4226 and RFC 6238.</td>
</tr>


<tr>
<td class="org-left">Database</td>
<td class="org-left"><a href="https://github.com/seancorfield/next-jdbc">seancorfield/next-jdbc</a></td>
<td class="org-left">Clojure wrapper for JDBC-based access to databases.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://www.postgresql.org/">PostgreSQL</a></td>
<td class="org-left">Object-relational database.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/tomekw/hikari-cp">tomekw/hikari-cp</a></td>
<td class="org-left">Clojure wrapper to HikariCP - "zero-overhead" production ready JDBC connection pool.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://www.h2database.com/html/main.html">H2</a></td>
<td class="org-left">Java SQL database running in PostgreSQL mode. Used when running tests.</td>
</tr>


<tr>
<td class="org-left">Time</td>
<td class="org-left"><a href="https://github.com/juxt/tick">juxt/tick</a></td>
<td class="org-left">Clojure(Script) library for dealing with time.</td>
</tr>


<tr>
<td class="org-left">Logging</td>
<td class="org-left"><a href="http://logback.qos.ch/">logback</a></td>
<td class="org-left">Logging framework.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/clojure/tools.logging">clojure/tools.logging</a></td>
<td class="org-left">Logging macros which delegate to a specific logging implementation.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/nberger/ring-logger">nberger/ring-logger</a></td>
<td class="org-left">Ring middleware to log response time and other details of each request that arrives to your server.</td>
</tr>


<tr>
<td class="org-left">Configuration</td>
<td class="org-left"><a href="https://github.com/yogthos/config">yogthos/config</a></td>
<td class="org-left">library for managing configuration using environment variables and EDN configuration files.</td>
</tr>


<tr>
<td class="org-left">Email</td>
<td class="org-left"><a href="https://github.com/drewr/postal">com.draines/postal</a></td>
<td class="org-left">Internet email library for Clojure.</td>
</tr>


<tr>
<td class="org-left">Testing</td>
<td class="org-left"><a href="https://github.com/lambdaisland/kaocha">lambdaisland/kaocha</a></td>
<td class="org-left">Full featured next generation test runner for Clojure.</td>
</tr>


<tr>
<td class="org-left">Linter</td>
<td class="org-left"><a href="https://github.com/clj-kondo/clj-kondo">clj-kondo</a></td>
<td class="org-left">A linter for Clojure code that sparks joy.</td>
</tr>


<tr>
<td class="org-left">Build tool</td>
<td class="org-left"><a href="https://clojure.org/guides/deps_and_cli">Deps and CLI</a></td>
<td class="org-left">The Clojure command line tools can be used to start a Clojure repl, use Clojure and Java libraries, and start Clojure programs.</td>
</tr>


<tr>
<td class="org-left">&#xa0;</td>
<td class="org-left"><a href="https://github.com/seancorfield/depstar">seancorfield/depstar</a></td>
<td class="org-left">Builds JARs, uberjars, does AOT, manifest generation, etc for deps.edn projects.</td>
</tr>
</tbody>
</table>


<a id="orgcb8f052"></a>

## Quick Start

Create a H2 DB seeded with a couple of Users:

    clj -M:dev-h2:seed-db

Start the REST API:

    clj -M:dev-h2:server

Use the Swagger UI at `http://localhost:3001/` to call the REST APIs.

Try `POST ​/api​/v0​/actions​/login` using `Basic bmVpbEB0ZW5mcmVuLmNvbToxMjM0NTY3OA==`

See the [PostgreSQL](#org6a5cf19) section for running PostgreSQL in a Docker container.


<a id="orgaf82086"></a>

## Usage


<a id="orgee44a7d"></a>

### Unset environment variables

If you have previously set any Tenfren environment variables in your current terminal, unset them by `source ./scripts/env-unset.sh`


<a id="org0f630f0"></a>

### Run the project in dev mode via the REPL

Use the `:dev` alias if you have [PostgreSQL setup](#org6a5cf19), otherwise use `:dev-h2` to use the H2 DB.

    $ clj -M:dev

When the REPL loads run `integrant.repl/go`

    user=> (go)

To reload the application after making changes run `integrant.repl/reset`

    user=> (reset)

Use the Swagger UI at `http://localhost:3001/` to call the REST APIs.


<a id="org4102151"></a>

### Run the project in server mode:

    $ clj -M:dev:ig-repl:server

Use the Swagger UI at `http://localhost:3001/` to call the REST APIs.


<a id="orgda89f07"></a>

### Run the project's tests:

    $ clj -M:test:test-kaocha

Run an individual test based on the meta-data associated with the test:

    $ clj -M:test:test-kaocha --focus-meta :login


<a id="org1c1be57"></a>

### Build an executable uberjar.

See the deps.edn `:uberjar` alias for details.

    $ clojure -X:uberjar

Run that uberjar:

    $ java -jar tenfren.jar

Use the Swagger UI at `http://localhost:3001/` to call the REST APIs.

There are also various bash scripts for the above commands.


<a id="org1e58142"></a>

## Logging


<a id="org703d9fb"></a>

### Dev

`./logs/tenfren-dev.log`


<a id="org9aa0688"></a>

### Testing

`./logs/tenfren-test.log`

See `./resources/config/<env>/logback.xml`.


<a id="orgd567a23"></a>

## REST API

The REST API aims to provide common application User Management services like:

-   Authentication
-   Authorization
-   Registration
-   Login with lockout after too many attempts
-   Password management
-   and CRUD APIs for Users and Admins.

I started out with the intent of only using REST APIs, but over time moved to a combination of HTTP RPC and REST APIs. Some operations, like `GET /api/v0/users/{email}` fit the REST design as it's a combination of a HTTP verb and an individually addressable resource. Other non-resource  operations like `/account/login` aren't a good fit for REST.

For these operations, [Google APIs](https://cloud.google.com/apis/design) append actions to the associated resource i.e. `https://service.name/v1/some/resource/name:customVerb` so in Google's [Firebase](https://firebase.google.com/docs/reference/rest/auth) a login action is `https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key`[API<sub>KEY</sub>]=. This seems like a good consistent way to define APIs, but for now I'm using the approach taken by [lipas](https://github.com/lipas-liikuntapaikat/lipas/blob/master/webapp/src/clj/lipas/backend/handler.clj) where actions/verbs are not associated with a resource, but take the simple format `/api/v0/actions/<action>`, so a login is `POST /api/v0/actions/login`.

A good starting point for REST API design alternatives is [API Design Patterns and Best Practices](https://www.moesif.com/blog/api-guide/api-design-guidelines)


<a id="orge9a26ef"></a>

### [Swagger](https://swagger.io/)

You can call the APIs via the Swagger UI.

[http://localhost:3001/index.html#/](http://localhost:3001/index.html#/)

![img](doc/images/swagger-api.png)


<a id="orgf94162c"></a>

### Usage

In Production these APIs should be called using HTTPS.


<a id="orgc6bca8f"></a>

#### Register a User

For an new User, creates a User with a `user` role and marks the Account status as inactive.
An email is sent containing instructions on how to complete the registration process.
The email includes a Time based OTP (one time password) that will expire after a configurable period of time.
For an existing inactive User, the same workflow is followed.


<a id="orgea1c176"></a>

##### Request

    curl -X 'POST' \
      'http://localhost:3001/api/v0/actions/register' \
      -H 'accept: application/json' \
      -H 'Content-Type: application/json' \
      -d '{
      "user/email": "neil@tenfren.com",
      "user/password": "12345678",
      "user/first-name": "Neil",
      "user/last-name": "Tenfren",
      "user/screen-name": "figgy"
    }'


<a id="org6e7a5de"></a>

##### Response - HTTP 201

Body

    {
      "message": "An email has been sent to neil@tenfren.com with the details for completing your Registration."
    }


<a id="org5b8dae0"></a>

##### The email

    neil@tenfren.com
    7:31 AM (0 minutes ago)
    to neil

    Welcome to Tenfren Neil
    To verify your email address, please enter the verification code below.

    The code will expire in 1 hour(s).

    724379

    If you didn’t make this request, don’t worry. Your account will not be created.

    Regards,

    The Tenfren Team.


<a id="org6113350"></a>

#### Confirming a Registration

Changes the Users status to active. Requires a valid Time based OTP (one time password) that was included in the Registration email.


<a id="org46ff4e5"></a>

##### Request

    curl -X 'PUT' \
      'http://localhost:3001/api/v0/actions/confirm-registration' \
      -H 'accept: application/json' \
      -H 'Content-Type: application/json' \
      -d '{
      "email": "neil@tenfren.com",
      "verification-code": 724379
    }'


<a id="org8e53dcf"></a>

##### Response - HTTP 200

    {
      "message": "Thanks for confirming your registation! You can now login."
    }


<a id="org43eb8bb"></a>

#### Login

Requires an Authentication header <type> <credentials>.
Where the type is `Basic` and the credentials are a Base64 encoded string with the format `email:password`. e.g. Basic bmVpbC5maWdnQGdtYWlsLmNvbToxMjM0NTY3OA==
The returned identitiy contains a signed JWT token used to authenticate the user in subsequent calls.


<a id="orgdb343ed"></a>

##### Request

    curl -X 'POST' \
      'http://localhost:3001/api/v0/actions/login' \
      -H 'accept: application/json' \
      -H 'authorization: Basic bmVpbC5maWdnQGdtYWlsLmNvbToxMjM0NTY3OA==' \
      -d ''


<a id="org783594e"></a>

##### Response - HTTP 200

    {
      "user/first-name": "Neil",
      "user/active": true,
      "user/id": 1,
      "user/token": "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ0ZW5mcmVuLmNvbSIsImlhdCI6MTYyODcxODI1NCwiZXhwIjoxNjYwMjc1ODU0LCJzdWIiOm51bGwsInVzZXIvaWQiOjEsInVzZXIvZW1haWwiOiJuZWlsLmZpZ2dAZ21haWwuY29tIn0.-WilX3FRkWvIVJg-xVpOHsjt3cHF_El11COiRAan7n9cIYLe_7V-rPYWpM19NgaBM29RuQt_Ye3Yo9qRftCb5g",
      "user/roles": [
        "admin",
        "user"
      ],
      "user/email": "neil@tenfren.com",
      "user/last-name": "Tenfren",
      "user/screen-name": "figgy"
    }


<a id="org797c37d"></a>

#### Change Password

The User has the JWT token after successfully logging in so they can now call APIs using the token for Authentication.

Requires an Authentication header <type> <credentials>.
Where the type is `Token` and the credentials are a signed JWT. e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..


<a id="orgfcd8b2f"></a>

##### Request

    curl -X 'PUT' \
      'http://localhost:3001/api/v0/actions/change-password' \
      -H 'accept: application/json' \
      -H 'authorization: Token eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ0ZW5mcmVuLmNvbSIsImlhdCI6MTYyODcxODI1NCwiZXhwIjoxNjYwMjc1ODU0LCJzdWIiOm51bGwsInVzZXIvaWQiOjEsInVzZXIvZW1haWwiOiJuZWlsLmZpZ2dAZ21haWwuY29tIn0.-WilX3FRkWvIVJg-xVpOHsjt3cHF_El11COiRAan7n9cIYLe_7V-rPYWpM19NgaBM29RuQt_Ye3Yo9qRftCb5g' \
      -H 'Content-Type: application/json' \
      -d '{
      "old-password": "12345678",
      "new-password": "abcdefgh"
    }'


<a id="org7707ea2"></a>

##### Response - HTTP 200

    {
      "message": "The password for neil@tenfren.com has been changed."
    }


<a id="orgba656a2"></a>

#### Request Password Reset

Sends an  email containing instructions on how to change the password.
The email includes a Time based OTP (one time password) that will expire after a configurable period of time.


<a id="orgaf25581"></a>

##### Request

    curl -X 'POST' \
      'http://localhost:3001/api/v0/actions/request-password-reset' \
      -H 'accept: application/json' \
      -H 'Content-Type: application/json' \
      -d '{
      "email": "neil@tenfren.com"
    }'


<a id="org22c3f1a"></a>

##### Response - HTTP 200

    {
      "message": "An email has been sent to neil@tenfren.com with the details for changing your password."
    }


<a id="orgaedfcd6"></a>

#### Reset Password

Changes the Users password. Requires a valid Time based OTP (one time password)
that was included in the Password reset email.


<a id="orgb1bc0ff"></a>

##### Request

    curl -X 'PUT' \
      'http://localhost:3001/api/v0/actions/reset-password' \
      -H 'accept: application/json' \
      -H 'Content-Type: application/json' \
      -d '{
      "email": "neil@tenfren.com",
      "password": "12345678",
      "verification-code": 831031
    }'
    }'


<a id="org239e253"></a>

##### Response - HTTP 200

    {
      "message": "The password for neil@tenfren.com has been changed."
    }

See <http://localhost:3001/index.html#/> for how to use the other APIs.


<a id="org8ccd67a"></a>

### Packaging


<a id="orge54070c"></a>

#### Build the executable uberjar

Using [seancorfield/depstar](https://github.com/seancorfield/depstar).

    clojure -X:uberjar :aliases [:dev]

This will generate `tenfren-api.jar`

Check that is built Ok. Note the PostgreSQL DB container should be up.

    java -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar tenfren-api.jar

To test <http://localhost:3001/index.html#/>.


<a id="orga9705da"></a>

#### Run the API in a Docker container

Environment variables are used to configure the API so export the following, or `source ./scripts/env-dev.sh`

    # API
    export AUTH_JWT_SECRET=somesecretkey
    export NOTIFIER_ENABLED=true
    export NOTIFIER_HOST=<smtp host>
    export NOTIFIER_USER=<your user name>
    export NOTIFIER_PASSWORD=<password>
    export NOTIFIER_FROM=neil@tenfren.com

The `docker-compose.yml` binds the environment variables to the below `api:`.

    version: '3.7'
    services:
      db:
        container_name: tenfren-db
        image: postgres:13
        ports:
          - "5432:5432"
        networks:
          [tenfren-network]
        environment:
          DB_PORT: 5432
          DB_NAME:
          DB_USER:
          DB_PASSWORD:
          DB_SUPERUSER:
          POSTGRES_PASSWORD: ${DB_SUPERUSER_PASSWORD}
        volumes:
          - pgdata:/var/lib/postgresql/data
          - pglog:/var/log
          - ./scripts/db-init.sh:/docker-entrypoint-initdb.d/db-init.sh
      api:
        container_name: tenfren-api
        build:
          context: .
          dockerfile: dockerfile-api
        ports:
          - "3000:3001"
        depends_on:
          - "db"
        networks:
          [tenfren-network]
        environment:
          DB_PORT: 5432
          DB_NAME:
          DB_USER:
          DB_PASSWORD:
          DB_URL:
          AUTH_JWT_SECRET:
          NOTIFIER_ENABLED:
          NOTIFIER_HOST:
          NOTIFIER_USER:
          NOTIFIER_PASSWORD:
          NOTIFIER_FROM:
    volumes:
      pgdata:
      pglog:
    networks:
      tenfren-network:
        name: tenfren-network

The `dockerfile-api` contains information such as the Java docker image to use, as well as  other configuation options for the API.

To check the `docker-compose.yml` file is correct.

    docker-compose config


<a id="orgb458c1c"></a>

#### Start the API Docker container

The PostgreSQL Docker container must be running. See the Database section for details.

This will start the `api:` service defined in the `docker-compose.yml`

    docker-compose up -d api

Show the running containers.

    docker container ls

or

    docker-compose ps

Additional ports info.

    sudo netstat -plnt

Check the logs for any issues

    docker-compose logs api


<a id="org6a2c387"></a>

#### Test with Swagger

Note that the port is 3000 and that Docker forwards traffic to the app running on 3001.

<http://localhost:3000/index.html>


<a id="org33732ab"></a>

#### Shell Access

For debugging and maintenance purposes you may want access the containers shell.

    docker exec -it tenfren-api bash

To exit

    exit


<a id="org585a1df"></a>

#### Stopping the container

    docker-compose -f docker-compose.yml stop api


<a id="org6a5cf19"></a>

## Database

Tenfren is configured to use either a `PostgreSQL` or `H2` DB.


<a id="orgdcebfa7"></a>

### Data Model

![img](doc/images/db-er.png)

In the Clojure code we have the `user` namespace. However, as `user` is a reserved word in PostgreSQL, that table is called `account`.

See `tenfren.db/map-table-ns` for how the namespace that `next.jdbc result_set` derives from the `account` table is changed to `user`.

e.g. internal namespace:

    {:user/id 1,
     :user/email neil@tenfren.com
     :user/roles [admin user]}


<a id="org2c550b2"></a>

### H2

Create a H2 DB seeded with a couple of Users:

    clj -M:dev-h2:seed-db

Start the REST API:

    clj -M:dev-h2:server

Use the Swagger UI at `http://localhost:3001/` to call the REST APIs.

Try `POST ​/api​/v0​/actions​/login` using `Basic bmVpbEB0ZW5mcmVuLmNvbToxMjM0NTY3OA==`

The H2 DB is created in `./db`


<a id="org6b0c3c3"></a>

### PostgreSQL

Running PostgreSQL in a Docker container.

See the [Troubleshooting](#orge55c834) section if any issues.


<a id="org1028874"></a>

### Docker


<a id="org8f5d915"></a>

#### Requirements

-   [Docker]
-   [PostgreSQL] Optional CLI `sudo apt-get install postgresql-client`

The client is optional as the PostgreSQL web based UI [pgadmin](https://www.pgadmin.org/) is a Docker service defined in the `docker-compose.yml`.


<a id="orgf955e1a"></a>

#### Local development:

-   Docker Configuration
-   Start the Docker container
-   Create the database user
-   Load the database schema
-   Create a test DB


<a id="org83ff4dc"></a>

#### Configuration

Environment variables are used to configure the DB so export the following, or `source ./scripts/env-dev.sh`

    # Postgres
    export DB_NAME=tenfren
    export DB_PORT=5432
    export DB_HOST=postgres
    export DB_SUPERUSER=postgres
    export DB_SUPERUSER_PASSWORD=password
    export DB_USER=tenfren
    export DB_PASSWORD=password

The `docker-compose.yml` binds the environment variables to the `db:` service..

    version: '3.7'
    services:
      db:
        container_name: tenfren-db
        image: postgres:13
        ports:
          - "5432:5432"
        networks:
          [tenfren-network]
        environment:
          DB_PORT: 5432
          DB_NAME:
          DB_USER:
          DB_PASSWORD:
          DB_SUPERUSER:
          POSTGRES_PASSWORD: ${DB_SUPERUSER_PASSWORD}
        volumes:
          - pgdata:/var/lib/postgresql/data
          - pglog:/var/log
          - ./scripts/db-init.sh:/docker-entrypoint-initdb.d/db-init.sh
      pgadmin:
        container_name: tenfren-db-pgadmin4
        image: dpage/pgadmin4
        ports:
          - "8082:80"
        networks:
          [tenfren-network]
        environment:
          PGADMIN_DEFAULT_EMAIL: "neil@tenfren.com"
          PGADMIN_DEFAULT_PASSWORD: "password"
      api:
        container_name: tenfren-api
        build:
          context: .
          dockerfile: dockerfile-api
        ports:
          - "3000:3001"
        depends_on:
          - "db"
        networks:
          [tenfren-network]
        environment:
          DB_PORT: 5432
          DB_NAME:
          DB_USER:
          DB_PASSWORD:
          DB_URL:
          AUTH_JWT_SECRET:
          NOTIFIER_ENABLED:
          NOTIFIER_HOST:
          NOTIFIER_USER:
          NOTIFIER_PASSWORD:
          NOTIFIER_FROM:
    volumes:
      pgdata:
      pglog:
    networks:
      tenfren-network:
        name: tenfren-network

To check the `docker-compose` file is correct:

    docker-compose config


<a id="org3c5ff3a"></a>

#### Start the DB Docker container

This will start the `db:` service defined in the `docker-compose.yml`

    docker-compose up -d db

Show the running containers.

    docker container ls

or

    docker-compose ps

Additional ports info.

    sudo netstat -plnt

Check the logs for any issues

    docker-compose logs db


<a id="org55c1cce"></a>

#### Create the database user

Run `./scripts/db-init.sh`.

This will drop/create the DB and user using the values from the environment variables exported earlier, when `docker-compose up -d db` is run.


<a id="org577fe49"></a>

#### Load the database schema

The password is `password`

    psql -h localhost -p 5432 -U postgres -d tenfren -f ./resources/db-schema/tenfren.ddl

This setup should lead to similar `:db-url` to include in the `config.edn`.

    :db-url "jdbc:postgresql://tenfren-db/tenfren?user=tenfren&password=password"


<a id="orgffc6e26"></a>

#### See the DB with Users

    export PGPASSWORD=password
    psql -h localhost -p 5432 -U postgres -d tenfren -f ./resources/db-schema/tenfren-account-insert.sql
    psql -h localhost -p 5432 -U postgres -d tenfren -f ./resources/db-schema/tenfren-role-insert.sql
    unset PGPASSWORD


<a id="org6ed4572"></a>

#### Database location

The `pgdata` volume to store the Postgres data we defined in the `docker-compose.yml` is located on my machine under `/var/lib/docker/volumes`.

    volumes:
      - pgdata:/var/lib/postgresql/data


<a id="orga70e50c"></a>

#### Log

Postgres is logging to `syserr` and the logs are available within the container via:

    docker logs <container id>

or

    docker-compose logs db

See <https://www.endpoint.com/blog/2014/11/12/dear-postgresql-where-are-my-logs>


<a id="orgea6083d"></a>

#### PostgreSQL Clients


<a id="org4075de0"></a>

##### SQL Command line

To run SQL commands from the command line.

    docker exec -it tenfren-db psql -U postgres -d tenfren

Check the Users were created

    select * from account;
    select * from role;

Userfull commands:

    \du
    \dt
    \q


<a id="orgd4bab1a"></a>

##### Pgadmin

    source ./scripts/env-dev.sh

Start the pgadmin container:

    docker-compose -f docker-compose.yml up -d pgadmin

Stop the pgadmin container:

    docker-compose -f docker-compose.yml stop pgadmin

Access the UI:

    http://localhost:8082/browser/#

Use the following values:

    Use the following:
    neil@tenfren.com
    user: tenfren
    password: password


<a id="org2053269"></a>

#### Shell Access

For debugging and maintenance purposes you may want access the containers shell.

    docker exec -it tenfren-db bash

To exit

    exit


<a id="orgba4804c"></a>

#### Stopping the container

    docker-compose -f docker-compose.yml stop db


<a id="org60dec51"></a>

#### Dumping the DB schema and data

    pg_dump -h localhost -p 5432 -U postgres -d tenfren -f ./resources/db-schema/tenfren-dump.ddl


<a id="orge55c834"></a>

#### Troubleshooting

When the container is started `./scripts/db-init.sh:/docker-entrypoint-initdb.d/db-init.sh` will only run if there is no data in the `pgdata volume`.

If there are any errors on startup, e.g. not all the env variables were exported, you will need to clear out the `pgdata` volume before `docker-compose up -d db` is called again.

Check the logs `docker logs tenfren-db` and if you see `PostgreSQL Database directory appears to contain a database; Skipping initialization`
even after you have cleared out the data, then you may have multiple containers running, and you may need to do the following&#x2026;

Remove tenfren-db containers that may be running

    docker container ls -qa
    docker container rm [id]

Remove tenfren volumes

    docker volume ls
    docker volume rm [VolumeName]


<a id="orgb0ac422"></a>

### Aggregating the SQL result sets (or any vector of maps).

See `tenfren.db/map-table-ns` for how/why we map the table ns from `account` to `user`.

There is a 1:M relationship between the `account` and `role` tables. When we query for a User that has multiple Roles we get back a SQL result set that is a vector of maps like:

    [{:user/id 1
      :user/email neil@tenfren.com
      :role/name admin}
     {:user/id 1
      :user/email neil@tenfren.com
      :role/name user}]

We want to aggregate the result to a single map with the roles in a vector like:

    [{:user/id 1,
      :user/email neil@tenfren.com
      :roles [admin user]}]

Using reduce:

    (defn reduce-user
      "TODO requiring 2 passes of the data 1) reduce 2) mapv. Can we do in one pass?"
      [users]
      (->> users
           (reduce
            (fn [acc m]
              (let [{:keys [user/id]} m]
                (if (contains? acc id)
                  (update-in acc [id :user/roles] (fnil conj []) (:role/name m))
                  (let [role (:role/name m)
                        m (-> m
                              (assoc :user/roles [role])
                              (dissoc :role/name))]
                    (assoc acc id m)))))
            {})
           (mapv (fn [m] (-> m val)))))


<a id="org37ba87c"></a>

## Configuration

Tenfren uses `config.edn` files and environment variables to configure the application using the [yogthos/config](https://github.com/yogthos/config) library.

The library will resolve config values in the following order:

-   `config.edn` on the classpath
-   Environment variables

`clj -M:dev` loads `./resources/config/dev/config.edn`.

`clj -M:test:test-kaocha` loads `./resources/config/test/config.edn`.

    (def default-config
      {:tenfren/db
       {:jdbc-url           (:db-url env)
        :adapter            (:db-adapter env)
        :database-name      (:db-name env)
        :server-name        (:db-host env)
        :port-number        (:db-port env)
        :username           (:db-user env)
        :password           (:db-password env)
        :auto-commit        (or (:db-auto-commit env) false)
        :connection-timeout (or (:db-connection-timeout env) 30000)
        :validation-timeout (or (:db-validation-timeout env) 5000)
        :idle-timeout       (or (:db-idle-timeout env) 600000)
        :max-lifetime       (or (:db-max-lifetime env) 1800000)
        :minimum-idle       (or (:db-minimum-idle env) 3)
        :maximum-pool-size  (or (:db-maximum-pool-size env) 4)
        :pool-name          (or (:db-pool-name env) "db-pool")}
       :tenfren/auth
       {:jwt-secret             (:auth-jwt-secret env)
        :jwt-opts               (or (:auth-jwt-opts env) {:alg :hs512})
        :jwt-token-expire-secs  (or (:auth-jwt-token-expire-secs env) 31557600)
        :totp-step-secs         (or (:auth-totp-step-secs env) 6000)
        :max-authentication-attempts (or (:auth-max-authentication-attempts env) 5)
        :authentication-lockout-ms   (or (:auth-authentication-lockout-ms env) 900000)
        :cors-allowed-origins   (or (:auth-cors-allowed-origins env)  ".*")
        :cors-max-age           (or (:auth-cors-max-age env) "600")
        :cors-allowed-headers   (or (:auth-cors-allowed-headers env) "Authorization, Content-Type")
        :cors-allowed-methods   (or (:auth-cors-allowed-methods env) "GET, PUT, PATCH, POST, DELETE, OPTIONS")
        :max-attempts           (or (:auth-max-attempts env) 3)}
       :tenfren/notifier
       {:enabled  (:notifier-enabled env)
        :host     (:notifier-host env)
        :user     (:notifier-user env)
        :password (:notifier-password env)
        :from     (:notifier-from env)}
       :tenfren/app
       {:db       (ig/ref :tenfren/db)
        :notifier (ig/ref :tenfren/notifier)
        :auth     (ig/ref :tenfren/auth)
        :version  (or (:app-version env) {:date "2021-08-01-v1"})}
       :tenfren/server
       {:app      (ig/ref :tenfren/app)
        :port     (or (:server-port env) 8091)}})

The environment state is then passed into the `handler.clj` using Integrant.

    (defmethod ig/init-key :tenfren/app [_ env]
      (t-handler/app env))

From there it is added to the HTTP Request via middleware

    (def env-middleware
      "Adds the environment to the request. The env keys are :db :notifier :auth."
      {:name ::env
       :compile (fn [{:keys [env]} _]
                  (fn [handler]
                    (fn [req]
                      (handler (assoc req :env env)))))})

The env is then availble to all routes and passed into core functions as an arg.

    :handler
        (fn [{{:keys [db]} :env}]
          {:status 200
           :body (t-core/get-users db)})

This [component and config discussion](https://clojureverse.org/t/stuart-sierra-components-and-config/7996) looks at alternatives for accessing config via functions, and stateful components wrapping config.
e.g.

    (def config
      (memoize
        (fn []
          (load-config ...))))


<a id="orgdc4a973"></a>

## Exception handling

Is handled by middleware that wraps all routes. See the middleware doco for details.


<a id="org24a718a"></a>

## Project layout

    $ tree -Ia 'doc|classes|script*|*.sh|target|.git'

The main file is `system.clj`.

![img](doc/images/project-layout.png)


<a id="org119051c"></a>

## Architecture

![img](doc/images/design.png)

![img](doc/images/deployment.png)


<a id="orgbb9a207"></a>

## Data formats

Metosin's muuntaja supports the following formats out of the box. just change the HTTP accept header to one of:

-   application/transit+json
-   application/transit+msgpack
-   application/json
-   application/edn


<a id="org3dc3e04"></a>

## Notifications

Some actions like requesting a password reset, or registration, generate an email that lets the User know how to finish the request.

You can configure your SMTP details in the `config.edn`. If you dont' want to send emails during development change the `:notifier-enabled` value to false, and the email text will be logged instead.

The test `config.edn` is already configured to log only.

Example Registration request email.

    neil@tenfren.com
    7:31 AM (0 minutes ago)
    to neil.figg

    Welcome to Tenfren Neil

    To verify your email address, please enter the verification code below.

    The code will expire in 1 hour(s).

    724379

    If you didn’t make this request, don’t worry. Your account will not be created.

    Regards,

    The Tenfren Team.


<a id="org968e806"></a>

## Middleware

Middleware are functions that add functionality to handlers. This makes the handlers simpler as they don't need to handle common concerns like authentication, authorization, and exception handling.

There is common middleware that adds functionality to all handlers e.g. error handling, content negotiation, and route specific middleware that adds function to specific routes e.g. authentication.


<a id="org582176e"></a>

#### Common middleware

The ordering of middleware is important and to see this order, uncomment the line `;:reitit.middleware/transform dev/print-request-diffs` in `handler.clj`

    :middleware [swagger/swagger-feature
                 t-mw/env-middleware
                 parameters/parameters-middleware
                 muuntaja/format-negotiate-middleware
                 muuntaja/format-response-middleware
                 t-mw/exception-middleware
                 muuntaja/format-request-middleware
                 muuntaja.middleware/wrap-params
                 coercion/coerce-response-middleware
                 coercion/coerce-request-middleware
                 t-mw/cors-middleware
                 #(logger/wrap-with-logger % {:redact-key? #{:password :user/password
                                                           :token :user/token
                                                           :token-key :user/token-key}})


<a id="org64502d4"></a>

##### Exception Handling middleware

See `tenfren.middleware/exception-middleware`. Custom exception handlers are merged with the default `reitit.ring.middleware.exception` handlers.

    (def exception-handlers
      {:invalid-token          (default-error-handler 401 :invalid-token)
       :invalid-security-code  (default-error-handler 401 :invalid-security-code)
       :account-locked         (default-error-handler 403 :account-locked)
       :no-permission          (default-error-handler 403 :no-permission)
       :password-mismatch      (default-error-handler 403 :password-mismatch)
       :user-not-found         (default-error-handler 404 :user-not-found)
       :email-not-found        (default-error-handler 404 :email-not-found)
       :reminder-not-found     (default-error-handler 404 :reminder-not-found)
       :registration-conflict  (default-error-handler 409 :registration-conflict)
       :email-conflict         (default-error-handler 409 :email-conflict)})

    (def exception-middleware
      (exception/create-exception-middleware
       (merge
        exception/default-handlers
        exception-handlers
        {::coercion/request-coercion (coercion-error-handler-400)
         ::coercion/reponse-coercion (coercion-error-handler-500)
         ::exception/default (error-handler-500)
         ::exception/wrap (fn [handler e request]
                            (log/error e (ex-message e))
                            (handler e request))})))


<a id="org506398a"></a>

##### `ring.logger` middleware used for logging HTTP requests.

-   Sensitive information like passwords can be redacted.

-   Muuntaja doesn't merge `:body-param=s into =:params` for performance reasons.

While `=ring.logger/wrap-with-logger` uses `:params` in debug mode to log the POST body.
So we need to add `wrap-params` as shown below which will  merge `:body-params` with `:params`.

tenfren-dev.log

    2021-08-12 07:55:17,356 [qtp1673765826-49] INFO  ring.logger - {:request-method :put, :uri "/api/v0/actions/change-password", :server-name "localhost", :ring.logger/type :starting}
    2021-08-12 07:55:17,356 [qtp1673765826-49] DEBUG ring.logger - {:request-method :put, :uri "/api/v0/actions/change-password", :server-name "localhost", :ring.logger/type :params, :params {:old-password "12345678", :new-password "abcdefgh"}}
    2021-08-12 07:55:17,823 [qtp1673765826-49] INFO  ring.logger - {:request-method :put, :uri "/api/v0/actions/change-password", :server-name "localhost", :ring.logger/type :finish, :status 200, :ring.logger/ms 467}


<a id="org6d078ba"></a>

##### `t-mw/env-middleware`

This middleware adds all the configuration required by Tenfren to the HTTP request. Specific middleware, and handler functions, then pull the config from the request. This way config is not global to applications functions but is passed in as an arg.


<a id="org27d58f7"></a>

#### Specific middleware

Specific middleware is associated with individual routes. For example, in the following `PATCH /api/v0/users/{email}` route, the middleware will..

-   `t-mw/token-auth-middleware`
    -   Check the `JWT` token contained in the `Authorization` header is valid. If valid, add the unsigned token details to the HTTP request under an `:identity` key.
-   `t-mw/auth-middleware`
    -   Check the request has been validated by checking the `:identity` key is not nil.
-   `t-mw/admin-middleware`
    -   Gets the User identity associated with the `:identity` key and checks they are an Admin. An Admin is a user with an `admin` role.

    ["/:email"
      :patch
         {:summary "Partially update a User's details - Requires Admin role."
          :description "Update a User's status to be `active` or `inactive` - Requires Admin role.
                       The User must be an Admin.
                       Requires an Authentication header <type> <credentials>.
                       Where the type is `Token` and the credentials are a signed JWT.
                       e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l"
          :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/admin-middleware]
          :parameters {:path t-schema/user-email
                       :body t-schema/user-status-patch
                       :header t-schema/auth-header}
          :responses  {200 {:body t-schema/user-summary}}
          :handler (fn [req]
                     (let [db (-> req :env :db)
                           resource-email (->> req :parameters :path :email (assoc {} :user/email))
                           user-details (-> req :parameters :body)
                           user (-> (t-core/get-user-by-email db resource-email)
                                    (select-keys [:user/id])
                                    (merge user-details))
                           _  (t-core/update-user-details db user)
                           user* (t-core/get-user-by-id db user)]
                       {:status 200
                        :body user*}))}]


<a id="org1cc0202"></a>

## Security

All routes requiring Authentication and Authorization checks are protected using `tenfren.middleware`.


<a id="orgee4aad7"></a>

### Authentication


<a id="orgbb0f941"></a>

#### JWT (Json Web Token)

When a User signs-in a JWT is created, signed, and added to the HTTP response. The JWT is then included in the HTTP requests as proof of identity for any route protected by `tenfren.middleware.token-auth-middleware`. Use `config.edn` to configure how long this JWT is valid for.

Example of a signed JWT

    eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ0ZW5mcmVuLmNvbSIsImlhdCI6MTYyODcxODI1NCwiZXhwIjoxNjYwMjc1ODU0LCJzdWIiOm51bGwsInVzZXIvaWQiOjEsInVzZXIvZW1haWwiOiJuZWlsLmZpZ2dAZ21haWwuY29tIn0.-WilX3FRkWvIVJg-xVpOHsjt3cHF_El11COiRAan7n9cIYLe_7V-rPYWpM19NgaBM29RuQt_Ye3Yo9qRftCb5g

The JWT when decoded looks like&#x2026;

Header: algorithm

    {
      "alg": "HS512"
    }

Payload: data

    {
      "iss": "tenfren.com",
      "iat": 1628718254,
      "exp": 1660275854,
      "sub": null,
      "user/id": 1,
      "user/email": "neil@tenfren.com"
    }


<a id="org3b73a1a"></a>

#### Time based One-Time Password (TOTP)

A [TOTP](https://www.twilio.com/docs/glossary/totp) is a unique numeric code generated using the IETF [RFC6238 algorithm](https://datatracker.ietf.org/doc/html/rfc6238), using the current time as an input. When a new User is created a unique key, used to generate TOTPs, is stored in the `account` table.
For any routes where an unauthenticated User makes a request, a TOTP is generated and used for authentication. For example, when a request is received to reset a password, the only information we have is the supplied email address. To prove the owner of the email address is making the request a TOTP is generated and sent to the User's supplied email address. The TOTP is then used to complete the password reset.

    Tenfren Password Reset
    Inbox

    neil@tenfren.com
    Thu, Aug 12, 8:06 AM (2 days ago)
    to me

    Welcome to Tenfren Neil
    To reset your new password, please enter the verification code below.

    The code will expire in 15 minute(s).

    831031

    If you didn’t make this request, don’t worry. Your account will not be changed.

    Regards,

    The Tenfren Team.


<a id="orgc4b65e0"></a>

### Authorization

A simple role-based approach is used for Authorization. Roles are stored in `role` table, with a 1:m relationship between `account` and `role`. A normal User has a `[user]` role, while an Admin has `[user,admin]` roles. The `tenfren.middleware.admin-middleware` is used to check if a User is authorized to access any Admin routes.

An example of a route protected by `admin-middleware`:

    (def user-routes
      ["/users"
       {:swagger {:tags ["Users"]}}
       [""
        {:get
         {:summary "Get all the Users - Requires Admin role."
          :description "Requires an Authentication header <type> <credentials>.
                       Where the type is `Token` and the credentials are a signed `JWT`.
                       e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
          :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/admin-middleware]
          :parameters {:header t-schema/auth-header}
          :responses  {200 {:body t-schema/user-summaries}}
          :handler
          (fn [{{:keys [db]} :env}]
            {:status 200
             :body (t-core/get-users db)})}


<a id="orgbd60462"></a>

## Testing

Unset any Tenfren environment variables.

`source ./scripts/env-unset.sh`

Run all the tests.

`clj -M:test:test-kaocha`

You should see output something like&#x2026;

    unit:   100% [==================================================] 6/6
    6 tests, 72 assertions, 0 failures.
    Ran tests.
    Writing HTML report to: /home/neil/data/github/tenfren/tenfren-api/target/coverage/index.html

    |--------------------+---------+---------|
    |          Namespace | % Forms | % Lines |
    |--------------------+---------+---------|
    |     tenfren.config |   76.97 |   90.91 |
    |       tenfren.core |   93.26 |   93.24 |
    |         tenfren.db |   98.11 |  100.00 |
    |    tenfren.handler |  100.00 |  100.00 |
    | tenfren.middleware |   95.11 |   96.69 |
    |   tenfren.notifier |   86.74 |   84.09 |
    |     tenfren.schema |  100.00 |  100.00 |
    |   tenfren.security |  100.00 |  100.00 |
    |     tenfren.system |   82.50 |   76.92 |
    |--------------------+---------+---------|
    |          ALL FILES |   93.92 |   95.37 |
    |--------------------+---------+---------|

To run individual tests e.g. login tests.

`clj -M:test:test-kaocha --focus-meta :login`

Where `:login` is meta-data attached to the test.

`(deftest ^:login login`

To change test behaviour modify the `tests.edn` file.

    #kaocha/v1
     {:kaocha/tests
      [{:kaocha.testable/id      :unit
        :kaocha.testable/type    :kaocha.type/clojure.test
        :kaocha/ns-patterns      ["-test$"],
        :kaocha/source-paths     ["src"],
        :kaocha/test-paths       ["test"],
        :kaocha.filter/skip-meta [:kaocha/skip]}]
      :kaocha/reporter #profile {:default kaocha.report.progress/report
                                 :d1 kaocha.report/dots
                                 :ci kaocha.report/documentation}
      :kaocha/color? #profile {:default true
                               :ci false}
      :kaocha/watch #profile {:default true
                              :ci false}
      :kaocha/fail-fast? true
      :kaocha/plugins
      [:kaocha.plugin/randomize
       :kaocha.plugin/filter
       :kaocha.plugin/capture-output
       :kaocha.plugin/cloverage
       :kaocha.plugin/profiling]
      :kaocha.plugin.capture-output/capture-output? true
      :kaocha.plugin.profiling/profiling? false
      :kaocha.plugin.profiling/count 10
      :kaocha.plugin.randomize/randomize? false
      :cloverage/opts
      {:ns-exclude-regex [],
       :text? false,
       :lcov? false,
       :high-watermark 80,
       :fail-threshold 0,
       :output "target/coverage",
       :low-watermark 50,
       :ns-regex [],
       :summary? true,
       :coveralls? false,
       :emma-xml? false,
       :html? true,
       :nop? false, ;; false to print result
       :codecov? false}}

An in-memory H2 database is used for testing. To change this see `./resources/config/test/config.edn`.


<a id="orgfe27a57"></a>

## Enhancements

-   encrypt the TOTP key stored for each User.
-   do we need to lock the User out if they provide the incorrect TOTP multiple times, or is a 15 minute time-to-live enough?
-   add a Clojurescript SPA using reagent and re-frame.
-   the README is an org file but I couldn't get the CircleCi link to render, so exporting as markdown.


<a id="org16fb545"></a>

## Bugs

-   TODO


<a id="org6e4787c"></a>

## Emacs Setup

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<tbody>
<tr>
<td class="org-left">IDE</td>
<td class="org-left"><a href="https://prelude.emacsredux.com/en/latest/">Emacs Prelude</a></td>
<td class="org-left">Prelude is an Emacs distribution that aims to enhance the default Emacs experience.</td>
</tr>
</tbody>
</table>

Alongside [CIDER](https://github.com/clojure-emacs/cider) I'm using [clojure-lsp](https://github.com/clojure-lsp/clojure-lsp), a Language Server for Clojure(script).

CIDER is enabled by default. To integrate with integrant add the following to `.dir-locals.el`

    ((clojure-mode
      (cider-ns-refresh-before-fn . "integrant.repl/suspend")
      (cider-ns-refresh-after-fn . "integrant.repl/resume")
      (cider-clojure-cli-global-options . "-A:dev")))

To enable clojure-lsp:

In `.emacs.d/personal/prelude-modules.el` uncomment `(require 'prelude-lsp)`

Prelude will load any `*.el` file in `.emacs.d/personal/` so I have `config-clojure-lsp.el` that contains:

    (add-hook 'clojure-mode-hook 'lsp)
    (add-hook 'clojurescript-mode-hook 'lsp)
    (add-hook 'clojurec-mode-hook 'lsp)


<a id="orgf9bb3f4"></a>

## Acknowledgements

-   <https://github.com/lipas-liikuntapaikat/lipas>
-   <https://github.com/karimarttila/clojure>
-   <https://github.com/metosin/reitit/tree/master/examples>
-   <https://github.com/seancorfield/usermanager-example/tree/polylith>
-   <https://github.com/prestancedesign/usermanager-reitit-example>
-   <https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app>
-   <https://github.com/seancorfield/dot-clojure>
-   <https://github.com/practicalli/clojure-deps-edn>


<a id="orgaddabc2"></a>

## Where does the name Tenfren come from?

In 2019 I had a Clojure(Script) app running on AWS that was a simple event planner that aimed to help a group of friends quickly find the best date(s) to have a catchup.

You know what it’s like to organise a get together with friends.

Everyone would like to come, but finding the best date takes time.

So the endless rounds of messages begin …

I though the app might be useful with groups of up to ten friends (Tenfren) but it didn't get any traction, so I've pulled out the user manager REST API for this repo.


<a id="org3abd924"></a>

## License

Copyright © 2018-2021 Neil Figg

Distributed under the MIT License.
