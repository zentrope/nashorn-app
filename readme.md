# scripto-app

_JDK9 compatible_

The Scripto App is a full-stack application (server and
web-application) exploring the use of JVM embedded scripting languages
as an integration extension mechanism.

## Build & Run

First, you can type:

    $ make help

to see available options, including re-setting the database, cleaning,
building, dependency analysis and interactive development.

To build and run the application:

    $ make build
    $ java -jar target/scripto-1.jar
    $ open http://localhost:2018

Note: If you don't have `make`, just read the `Makefile` to see the
commands it uses: a combination of `leiningen` and `bash`. For this
app, `make` and `Makefile` are just convenience shell wrappers.

## Scripting language support

The following scripting languages are supported:

- JavaScript (via OpenJDK 8 Nashorn support)
- Python (via Jython 2.7.1b3)

## Rationale

One way to use embedded scripting languages is to expose
domain-specific functions to a scripting engine so that users can add
more features to the host application. For instance, Emacs (Emacs
Lisp) is built around this idea, as are many editors, such as Sublime
Text (Python) and Atom (JavaScript) and other rich applications such
as CAD software.

What _this_ particular application emphasizes is using small scripts
to orchestrate and integrate remote third-party web services. Even if
hosted inside a monolithic Enterprise application, the scripts don't
interact with the host-app except via its public web service.

### Functions

The functionality exposed to the scripts are web `GET` and `POST`
functions as well as convenience functions for formatting, parsing
and system-wide key/value configuration property lookup.

The context is that these scripts encourage users of a host
application with a web API (REST or GraphQL) to write integrations
(from app to third-party apps) hosted by the app itself rather than
via additional processes schedule via (say) cron.

With even just what's implemented here, you could:

- Request data from the host-app and push to a third-party ticketing system.
- Request third-party data and push to the host-app.
- Invoke host-app functions (via POST) at regular intervals.
- Configure the host-app according to config found at a third-party service.

And so on, depending on what the host application provides by way of
HTTP compatible end-points.

### Properties

Also supported is the notion of a directory of `properties`: named
URLs, API authorization tokens, certificates, or other parameters one
or more scripts might need to use.

### Scheduling

Finally, the app provides a scheduler so that any (or all) scripts can
be scheduled to run at a given time. The results are logged (stdout,
errors and returned values), but the intention is for scripts is move
data from one place to another, not to provide a user interface or
database for storing data locally.
