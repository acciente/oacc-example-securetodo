SecureTodo OACC Example Application
===================================

Welcome to the SecureTodo OACC example repository. The SecureTodo application serves as an example on how to integrate the open-source OACC security framework into a sample Java application to address several real-world authorization scenarios.

### How to use this repo

The SecureTodo example application is a RESTful Java implementation of a _secured_ todo list. It uses the Dropwizard framework to stand up a RESTful web service, and the OACC framework to provide application security.

While you could simply browse the source code directly from this repo to see how OACC does its magic, there's a better alternative:

#### Code Walkthrough

The accompanying [**code walkthrough document**](walkthrough/secure-todo-example.md) is part of this repo and explains the SecureTodo example application and the relevant OACC features, in detail.

### What is OACC, anyways?

OACC - pronounced _[oak]_ - is a fully featured API to both **enforce** and **manage** your application's authentication and authorization needs.

You can find more information about the OACC Java Security Framework, including the latest Javadocs, releases, and tutorials on the project website:
[oaccframework.org](http://oaccframework.org).

Running the example
-------------------

### Supported Environments

The SecureTodo sample application is compatible with Java™ SE 8 (Java™ version 1.8.0), or higher.

### How to start the SecureTodo application

1. Run `mvn clean package` to package the application
2. Start the application with `java -jar target/secure-todo-1.0.0-SNAPSHOT.jar server secure-todo.yml`

### curl API commands

To interact with the SecureTodo API using _curl_ try some of the sample commands below:

- POST a new user:  

    ```bash
    curl -i -k --silent -w "\n" \
    -H "Content-Type: application/json" \
    -X POST -d '{"email":"alice@oaccframework.org", "password":"secret"}' \
    https://localhost:8443/users
    ```

- POST a new todo for the authenticated user:

    ```bash
    curl -i -k --silent -w "\n" \
    -u alice@oaccframework.org:secret \
    -H "Content-Type: application/json" \
    -X POST -d '{"title":"wash car"}' \
    https://localhost:8443/todos
    ```

- GET todos for the authenticated user:

    ```bash
    curl -i -k -w "\n" \
    -u alice@oaccframework.org:secret \
    https://localhost:8443/todos
    ```

- PATCH an existing todo:

    ```bash
    curl -i -k --silent -w "\n" \
    -u alice@oaccframework.org:secret \
    -H "Content-Type: application/json" \
    -X PATCH -d '{"completed":"true"}' \
    https://localhost:8443/todos/1
    ```

- PUT a share-request for an existing todo:

    ```bash
    curl -i -k --silent -w "\n" \
    -u alice@oaccframework.org:secret \
    -H "Content-Type: application/json" \
    -X PUT https://localhost:8443/todos/1/?share_with=bob@oaccframework.org
    ```

License
-------

The SecureTodo sample application is open source software released under the commercial friendly [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0). The [accompanying document](walkthrough/secure-todo-example.md) - including the illustrations referenced within it - that walks through and explains the SecureTodo code is licensed under a [Creative Commons Attribution 4.0 International License (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/).


About Acciente
--------------

[Acciente, LLC](http://www.acciente.com) is a software company located in Scottsdale, Arizona specializing in systems architecture and software design for medium to large scale software projects.