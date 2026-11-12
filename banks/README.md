# Officiële vragenbanken – groep 7/8

Vragen voor groep 7 en 8 van de Nederlandse basisschool (voorbereiding op de doorstroomtoets).
In de App：「设置 → 管理题库 → 选择官方题库」，一键即可使用。

| 题库 | 内容 | 远程网址 |
|---|---|---|
| **Groep 7/8 – Alles** | 下面四份合并 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/all.json` |
| Taal | werkwoordspelling (d/t), spelling, meervoud/verkleinwoord, woordsoorten, zinsontleding, spreekwoorden | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-taal.json` |
| Rekenen | breuken, procenten, kommagetallen, meten, tijd, schaal, verhoudingen | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-rekenen.json` |
| Wereldoriëntatie | provincies en hoofdsteden, geschiedenis, natuur | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-wereld.json` |
| Engels | Engelse woordjes, zinnen, eenvoudige grammatica | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-engels.json` |

## 添加或修改题目

**最简单：直接在 GitHub 网页上改**

1. 打开要改的 `nl-*.json` → 点右上角 ✏️（Edit）→ 修改 → **Commit changes**
2. GitHub Actions 会自动校验题目，并重新生成 `all.json`、提交回仓库（约 1 分钟）
3. App 会在 6 小时内自动拿到新题（也可以在题库管理里点「保存并更新」立即刷新）

新建题库：在 `banks/` 里新建 `nl-xxx.json`，会自动合并进 `all.json`。

**也可以在电脑上改**：改完运行 `python3 tools/build_banks.py`，再提交推送（不运行也没关系，GitHub 会自动生成）。

> 题目格式有错时，Actions 会显示红色 ❌ 并指出第几题出错，`all.json` 不会被更新，App 里的题库也不受影响。
> GitHub 的 raw 网址有几分钟缓存，更新后稍等再刷新。
> `all.json` 是自动生成的，不要手动编辑。
