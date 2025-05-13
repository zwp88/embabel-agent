# Embabel Agent API

Root package for Embabel Agent API.

Subpackages:

- `api`: Public API. Your apps should primarily use this package. The two key subpackages are fully interoperable:
    - `annotation`: Spring-style annotations defining agents and actions.
    - `dsl`: Kotlin DSL for defining agents and actions.
    - `common`: Common classes used in both annotation and DSL idioms.
- `core`: Core classes of the Agent API. These will appear in the public API
  but some are lower level than the public API.
- `domain`: Data dictionary of com
- `spi`: Defines types primarily used by platform providers. Not intended for user use.
- `testing`: Testing support classes. Intended to make it easy for application developers to unit and integration test
  their agents.