这里是使用 uv + Poetry 创建新 Python 项目的完整流程：

## 前置准备

首先确保安装了必要工具：
```bash
# 安装 uv（如果还没安装）
curl -LsSf https://astral.sh/uv/install.sh | sh

# 安装 Poetry（如果还没安装）
curl -sSL https://install.python-poetry.org | python3 -
```

## 项目创建流程

### 1. 创建项目目录并初始化
```bash
# 创建新项目
poetry new my-project
cd my-project

# 或者在现有目录中初始化
mkdir my-project && cd my-project
poetry init
```

### 2. 配置 Poetry 使用 uv
```bash
# 配置 Poetry 使用 uv 作为包安装器
poetry config installer.modern-installation false
poetry config installer.parallel true

# 可选：配置虚拟环境位置
poetry config virtual.in-project true
```

### 3. 指定 Python 版本
```bash
# 使用 uv 安装特定 Python 版本
uv python install 3.12

# 在 pyproject.toml 中指定 Python 版本要求
# 编辑 pyproject.toml 文件
```

### 4. 安装依赖和创建虚拟环境
```bash
# Poetry 会自动使用 uv 来解析和安装依赖
poetry install

# 添加新依赖
poetry add fastapi uvicorn
poetry add pytest --group dev
```

### 5. 激活虚拟环境
```bash
# 进入 Poetry shell
poetry shell

# 或者直接运行命令
poetry run python main.py
```

## 项目结构示例

创建完成后的典型项目结构：
```
my-project/
├── pyproject.toml
├── README.md
├── my_project/
│   └── __init__.py
├── tests/
│   └── __init__.py
└── .venv/  # 如果配置了 in-project
```

## pyproject.toml 配置示例

```toml
[tool.poetry]
name = "my-project"
version = "0.1.0"
description = ""
authors = ["Your Name <you@example.com>"]

[tool.poetry.dependencies]
python = "^3.12"
fastapi = "^0.104.0"

[tool.poetry.group.dev.dependencies]
pytest = "^7.4.0"
black = "^23.0.0"
isort = "^5.12.0"

[build-system]
requires = ["poetry-core"]
build-backend = "poetry.core.masonry.api"
```

## 使用 uv 的优势

1. **更快的依赖解析**：uv 比传统的 pip 快 10-100 倍
2. **更好的依赖管理**：更准确的依赖解析算法
3. **内存效率**：使用 Rust 编写，内存占用更小

## 常用命令

```bash
# 查看项目信息
poetry show

# 更新依赖
poetry update

# 运行脚本
poetry run python script.py

# 构建项目
poetry build

# 发布到 PyPI
poetry publish
```

这种组合方式让您能够享受 Poetry 优秀的项目管理功能和 uv 的高性能包管理，是目前 Python 项目开发的最佳实践之一。