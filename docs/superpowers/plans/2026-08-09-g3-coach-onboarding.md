# G3 教练入驻与审核（US-010 / US-011 / US-040）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现教练端入驻资料提交（含草稿/正式提交/重新入驻）与管理后台入驻资质审核的完整后端 API + 教练端小程序页面 + web-admin 审核页面。

**Architecture:** 后端在现有 Spring Boot + MyBatis-Plus + Flyway 架构上新增 coach_application / coach_certificate_application / coach_certificate / coach_audit_log 四表，通过 CoachOnboardingService 处理入驻状态机与快照，通过 CoachAuditService 处理管理员审核；教练端小程序使用 Taro React + Zustand 新增 pages/onboarding/* 三页面；web-admin 使用 Vue 3 + Element Plus + Pinia 新增教练审核列表/详情页面。

**Tech Stack:** Java 21 / Spring Boot 3.2 / MyBatis-Plus / Flyway / H2 & MySQL / Taro React / Vue 3 / Element Plus / Pinia / Zustand
