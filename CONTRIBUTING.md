# Contributing to Levtus

First off, thank you for considering contributing to Levtus! It's people like you who make the open-source community such an amazing place to learn, inspire, and create.

As we aim for publication on Maven Central, we maintain high standards for code quality, security, and documentation.

## 📜 Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct (link to be added or follow standard Contributor Covenant).

## 🛠 How Can I Contribute?

### Reporting Bugs
* Check the existing issues to see if the bug has already been reported.
* If not, open a new issue. Use a clear title and provide as much context as possible (steps to reproduce, expected behavior, actual behavior, JVM version).

### Suggesting Enhancements
* Open an issue to discuss the enhancement before starting work.
* Explain the use case and how it benefits the project.

### Pull Requests
1. Fork the repository and create your branch from `main`.
2. If you've added code that should be tested, add tests!
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code follows the style guidelines.
6. Sign your commits with GPG.

## 🏗 Development Guidelines

### Java Version
Levtus targets **Java 25**. Ensure your development environment is set up accordingly.

### Code Style
We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
* Use 2 spaces for indentation.
* Organize imports (no wildcard imports).
* Use meaningful variable and method names.

### Documentation
* All public classes and methods **must** have descriptive JavaDoc.
* Explain *why* something is done, not just *what* is being done if the logic is complex.

### Testing
* We use **JUnit 5**.
* Every new feature or bug fix should be accompanied by relevant unit or integration tests.
* Place tests in `src/test/java`.

### Commit Messages
We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
* `feat`: A new feature
* `fix`: A bug fix
* `docs`: Documentation only changes
* `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc)
* `refactor`: A code change that neither fixes a bug nor adds a feature
* `test`: Adding missing tests or correcting existing tests
* `chore`: Changes to the build process or auxiliary tools and libraries

Example: `feat: add support for multipart/form-data`

### GPG Signing
To ensure the integrity and authenticity of contributions, all commits must be GPG-signed.
* [How to sign commits with GPG](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits)

## 🚀 Branching Strategy
We use **GitHub Flow**:
* `main` is the production-ready branch.
* All work happens in feature branches (`feature/my-cool-feature`) or bugfix branches (`bugfix/fix-memory-leak`).
* Pull Requests are merged into `main` after review and passing CI checks.

## 📦 Maven Central Requirements
To ensure we can publish to Maven Central, please ensure:
* `pom.xml` information is kept up to date.
* No external dependencies are added without prior discussion (Levtus aims to be zero-dependency).
* Javadoc and Source JARs are generated correctly (handled by build plugins).

---

Thank you for your contribution!
