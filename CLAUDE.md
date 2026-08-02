# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository purpose

This is not a software project — it is a personal study-notes repository for iSAQB CPSA-F (Certified Professional for Software Architecture – Foundation Level) exam preparation. There is no source code, no build system, and no tests. There are no build, lint, or test commands to run.

## Structure

- `isaqb-study-prep.md` — the main (currently only) study document: a tiered checklist of architectural patterns to master for the exam (Layers, Pipes and Filters, Microservices, Broker, SOA, Ports and Adapters, CQRS, Event Sourcing, EIP/messaging patterns, etc.), organized by exam priority (R1 = must-apply, R3 = must-explain) and tagged with relevance to the author's CI/CD and Kubernetes work experience.

## Working in this repository

- Treat requests here as requests to write, expand, or refine study/reference material — not to implement software.
- When adding a new pattern or topic, follow the existing structure in `isaqb-study-prep.md`: a **Understand** bullet (what to know) and a **Why** bullet (why it matters for the exam and/or the author's job context), grouped under the existing Tier 1/2/3 headings by exam priority.
- Keep the author's stated exam-prep framing intact: each pattern should be answerable in terms of (1) problem/context, (2) structure, (3) quality attributes helped/hurt, (4) when not to use it, (5) a concrete example from the author's CI/CD, Kubernetes, or past Java work.
