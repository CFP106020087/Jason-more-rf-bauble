#!/usr/bin/env python3
"""
重构 MechanicalCoreHUD.java 的 NBT 键名
"""
import re

def refactor_hud_nbt(content):
    """替换 HUD 中的 NBT 键名模式"""

    # Pattern: nbt.getBoolean("Disabled_" + var) -> nbt.getBoolean(UpgradeKeys.kDisabled(var))
    content = re.sub(
        r'nbt\.getBoolean\("Disabled_"\s*\+\s*(\w+)\)',
        r'nbt.getBoolean(UpgradeKeys.kDisabled(\1))',
        content
    )

    # Pattern: nbt.getBoolean("Disabled_CONSTANT") -> nbt.getBoolean(UpgradeKeys.kDisabled("CONSTANT"))
    content = re.sub(
        r'nbt\.getBoolean\("Disabled_([A-Z_]+)"\)',
        r'nbt.getBoolean(UpgradeKeys.kDisabled("\1"))',
        content
    )

    # Pattern: key.startsWith("Disabled_") -> key.startsWith(UpgradeKeys.K_DISABLED_PREFIX)
    # 这个需要在 UpgradeKeys 中定义 K_DISABLED_PREFIX 常量，或者保持原样
    # 为了简化，我们保持这个不变

    return content

def main():
    file_path = 'src/main/java/com/moremod/client/gui/MechanicalCoreHUD.java'

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_lines = len(content.splitlines())

    # 执行替换
    new_content = refactor_hud_nbt(content)

    # 统计替换次数
    changes = sum(1 for old, new in zip(content.splitlines(), new_content.splitlines()) if old != new)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"✓ HUD 替换完成")
    print(f"  文件: {file_path}")
    print(f"  行数: {original_lines}")
    print(f"  修改行数: {changes}")

if __name__ == '__main__':
    main()
