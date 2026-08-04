# Claude Code Configuration for PkCrates

> AI-powered development workspace configuration

## Available Skills

Skills are loaded from `.claude/skills/` (symlinked from claude-code-java).

To use a skill, load it first, then invoke with natural language:

### 1. Git Commit Messages
**Load**: `view .claude/skills/git-commit/SKILL.md`

**Use cases**:
- "Commit staged changes"
- "Create commit for bug fix #123"
- "Generate conventional commit message"

**Example**:
```
> view .claude/skills/git-commit/SKILL.md
> "Commit these changes"
→ fix(plugin-loader): prevent NPE when directory missing
```

### 2. Test Quality (JUnit 5 + AssertJ)
**Load**: `view .claude/skills/test-quality/SKILL.md`

**Use cases**:
- "Add tests for PluginManager.loadAll()"
- "Review existing tests in PluginLoaderTest"
- "Improve test coverage for lifecycle module"

**Example**:
```
> view .claude/skills/test-quality/SKILL.md
> "Add unit tests for ExtensionFactory with edge cases"
→ Generates JUnit 5 tests with AssertJ assertions
```

### 3. Issue Triage
**Load**: `view .claude/skills/issue-triage/SKILL.md`

**Use cases**:
- "Triage the last 10 issues"
- "Check recent bug reports"
- "Prioritize open feature requests"

**Example**:
```
> view .claude/skills/issue-triage/SKILL.md
> "Triage issues from PkCrates, last 15"
→ Categorizes, labels, suggests responses
```

## MCP Servers (Optional)

MCP servers enhance capabilities with structured, token-efficient operations:

| Server | Benefits |
|--------|----------|
| GitHub MCP | Issue management, PR creation |
| Filesystem MCP | Structured file tree navigation |
| Git MCP | Commit history, blame, log parsing |

To configure MCP servers, run from claude-code-java:
```bash
./scripts/configure-mcp.sh /path/to/this/project
```

See [MCP documentation](https://modelcontextprotocol.io/) for details.

## Common Workflows

### Daily Development Flow
```bash
# 1. Start session
claude code .

# 2. Work on feature/fix
# ... make code changes ...

# 3. Add tests (load test-quality skill)
> view .claude/skills/test-quality/SKILL.md
> "Add tests for new functionality in class X"

# 4. Commit (load git-commit skill)
> view .claude/skills/git-commit/SKILL.md
> "Commit staged changes"

# 5. Push and create PR
> "Push changes and create PR for issue #123"
```

### Weekly Maintenance
```bash
# Monday morning: Issue triage
claude code .

> view .claude/skills/issue-triage/SKILL.md
> "Triage the last 20 issues, categorize and prioritize"

# Review suggested actions
> "Apply labels and post responses as suggested"
```

### Code Review
```bash
# Review PR
> "Review PR #456 focusing on:
   - Test coverage (use test-quality skill)
   - Commit message quality (use git-commit skill)
   - Code patterns and best practices"
```

## Token Budget Guidelines

To optimize token usage:

1. **Load skills once per session** - Skills stay in context
2. **Batch operations** - Process multiple issues/tests together
3. **Use MCP when available** - More efficient than bash commands
4. **Targeted file reads** - Only read files you need

### Target Token Usage

| Task | Without Skills | With Skills | Savings |
|------|----------------|-------------|---------|
| Commit message | ~800 tokens | ~300 tokens | 62% |
| Add 3 tests | ~2000 tokens | ~800 tokens | 60% |
| Triage 10 issues | ~5000 tokens | ~2000 tokens | 60% |

## What to Avoid

1. **Don't reload skills repeatedly** - Load once per session
2. **Don't process issues one-by-one** - Batch them
3. **Don't over-engineer** - Use skills for appropriate tasks
4. **Don't ignore skill guidelines** - They're optimized for tokens

## Project-Specific Notes

### Build Commands

This is a **Gradle** project (Kotlin DSL), not Maven. Always use the wrapper.

```bash
./gradlew build          # compiles + produces the shaded jar in build/libs/
./gradlew compileJava    # fast compile-only check
```

- Java 21 toolchain, Paper 1.21.4 API, `paper-plugin.yml` (not `plugin.yml`).
- Never set `org.gradle.java.home` in `gradle.properties` — the toolchain block handles it.

### Dependencies

**Nothing is shaded.** Every dependency is `compileOnly`. Runtime libraries are resolved by
`PkCratesLoader` via Paper's `MavenLibraryResolver` against the server's `libraries/` cache.

To add a runtime library, change **two** places with identical coordinates:

1. `compileOnly("group:artifact:version")` in `build.gradle.kts`
2. The `RUNTIME_LIBRARIES` array in `PkCratesLoader.java`

Forgetting step 2 compiles cleanly and dies at runtime with `NoClassDefFoundError`.
Note `paper-plugin.yml` has no `libraries:` key — that is `plugin.yml` only; the loader
class is the supported path here.

### Testing Strategy

**Current state: there is no test source set and no test dependency.** Adding one is the
first step for any coverage work — do not assume `./gradlew test` does anything today.

- Target once bootstrapped: 80%+ on core logic (`core/service`, `core/task`, `core/model`)
- Focus: reward roll weighting, claim delivery atomicity, key consumption, mass opening queue
- Tools: JUnit 5, AssertJ, Mockito

### Threading Rules

Bukkit API calls and event dispatch are **main-thread only**. Virtual-key lookups return
futures that complete on the `PkCrates-DB` thread, so any continuation that touches a
player, an inventory, or fires an event must be chained with
`thenApplyAsync(fn, mainThread)` using `infrastructure.scheduler.MainThreadExecutor`.

### Commit Guidelines
- Follow Conventional Commits
- Reference issues: "Fixes #123"
- Keep subject under 50 chars

### Issue Management
- Label all new issues within 48h
- Respond to questions within 1 week
- Close stale (>90 days, no activity) issues

## Resources

- [claude-code-java](https://github.com/decebals/claude-code-java) - Skill repository
- [Claude Code Docs](https://code.claude.com/docs) - Official documentation
- [Conventional Commits](https://www.conventionalcommits.org/) - Commit format
- [AssertJ Docs](https://assertj.github.io/doc/) - Assertion library

## Tips & Tricks

### Quick skill loading
```bash
# Add to your shell alias
alias cc-commit='echo "view .claude/skills/git-commit/SKILL.md"'
alias cc-test='echo "view .claude/skills/test-quality/SKILL.md"'
alias cc-triage='echo "view .claude/skills/issue-triage/SKILL.md"'
```

### Session continuity
```bash
# Save context at end of session
> "Summarize what we worked on today for next session"

# Resume next day
> "Review yesterday's summary and continue"
```

### Measure your wins
```bash
# Track token usage
> /token usage

# Compare before/after adopting skills
# Document savings in team retrospectives
```

---

**Last updated**: 2026-08-02
**claude-code-java version**: v0.1
