# 灵光乍现记录

## 1. AI 的commands
- 是不是可以有什么commands 可以让AI规范的去创建需求，先写需求文档，在拆分用户故事，再同步openspec中的文档，再更新index.md，再设计原型设计，等一系列的文档输出的内容

## 2. 像docs/spec中的内容，是不是可以作为AI的prompt，来规范的创建需求文档，比如是commands或者其他的，在agents.md中指定如果涉及到创建需求或者创建用户故事相关的内容，执行命令或者规范，就可以根据规范创建相关的内容
- 比如，创建需求文档的command 是 `create-requirement-doc`，在agents.md中指定如果涉及到创建需求文档的内容，执行这个command，就可以根据spec中的内容，创建出符合要求的需求文档
- 比如，创建用户故事的command 是 `create-user-story`，在agents.md中指定如果涉及到创建用户故事的内容，执行这个command，就可以根据spec中的内容，创建出符合要求的用户故事

## 3. calicat将原型设计完成之后，那需要将链接更新到对应的page-spec中，这个步骤是不是可以提炼成什么tool或者commands执行

## 4. 一个批次的需求设计完成之后，需要进行业务评审，需要梳理，梳理成第四批次页面梳理，然后更新对应用户故事的状态，为review, 检查逻辑有没有问题，如果检查无误，将状态都更新为APPROVAL，然后根据这个批次要做的用户故事，生成开发计划文档，放在docs/tech/dev-plan-batchX.md，随后就要根据这个文档开始依次进入代码开发，测试，验证阶段了
- 比如，一个批次的需求设计完成之后，进行业务评审，然后更新对应用户故事的状态，从 `[DRAFT]` 改为 `[REVIEW]` 或者 `[APPROVAL]`