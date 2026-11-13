# Contributing to cs4-game

Bug reports, test additions, and documentation fixes are welcome. For larger changes, open an issue first so we can agree on the scope.

## Before you start

- Search [existing issues](https://github.com/andrew-abdelmalak/cs4-game/issues) to avoid duplicates.
- By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Set up your environment

Requirements: Java 17 (JDK 17 or later) and Apache Maven 3.9+.

```bash
git clone https://github.com/andrew-abdelmalak/cs4-game.git
cd cs4-game
mvn test-compile
```

## Run the checks

```bash
mvn clean test
mvn -q exec:java -Dexec.args="--demo"
```

CI runs `mvn -B clean test` (with `-Xlint:all -Werror` on main sources) and the `--demo` runner on every pull request. A pull request with failing checks cannot be merged.

Note: `M1PublicTests` and `M2PublicTests` verify the exact package, class, field, and method signatures of the course specification using Java reflection. Keep existing public APIs intact when changing engine internals.

## Make your change

1. Create a branch from `main`: `git switch -c fix/short-description`.
2. Keep each change focused on one problem.
3. Add or update JUnit 4 tests in `src/tests/` for behavior changes.
4. Update `README.md` when commands, data schemas, or gameplay behavior change.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/): `type(scope): summary`, for example `fix(medic): allow healing full-HP targets`.

## Open a pull request

1. Push your branch and open a pull request against `main`.
2. Fill in the pull request template, and link any related issue (`Fixes #123`).

## Report a bug or request a feature

Use the [issue forms](https://github.com/andrew-abdelmalak/cs4-game/issues/new/choose). For security problems, follow [SECURITY.md](SECURITY.md) instead. Never report vulnerabilities in a public issue.

## License

By contributing, you agree that your contributions are licensed under the project's [MIT License](../LICENSE).
