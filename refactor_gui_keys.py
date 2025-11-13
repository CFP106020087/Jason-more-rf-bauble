#!/usr/bin/env python3
"""
重构 MechanicalCoreGui.java 的 NBT 键名 - 简单替换版本
只替换单变体的简单模式，保留复杂模式供手动处理
"""
import re

def refactor_gui_nbt(content):
    """替换 GUI 中的简单 NBT 键名模式"""

    replacements = [
        # Single-variant setInteger patterns
        (r'nbt\.setInteger\("OwnedMax_"\s*\+\s*(\w+),', r'nbt.setInteger(UpgradeKeys.kOwnedMax(\1),'),
        (r'nbt\.setInteger\("OriginalMax_"\s*\+\s*(\w+),', r'nbt.setInteger(UpgradeKeys.kOriginalMax(\1),'),
        (r'nbt\.setInteger\("upgrade_"\s*\+\s*(\w+),', r'nbt.setInteger(UpgradeKeys.kUpgrade(\1),'),

        # Single-variant setBoolean patterns
        (r'nbt\.setBoolean\("HasUpgrade_"\s*\+\s*(\w+),', r'nbt.setBoolean(UpgradeKeys.kHasUpgrade(\1),'),
        (r'nbt\.setBoolean\("IsPaused_"\s*\+\s*(\w+),', r'nbt.setBoolean(UpgradeKeys.kPaused(\1),'),
        (r'nbt\.setBoolean\("Disabled_"\s*\+\s*(\w+),', r'nbt.setBoolean(UpgradeKeys.kDisabled(\1),'),
        (r'nbt\.setBoolean\("WasPunished_"\s*\+\s*(\w+),', r'nbt.setBoolean(UpgradeKeys.kWasPunished(\1),'),

        # Single-variant getInteger patterns (simple, not in Math.max)
        (r'nbt\.getInteger\("OwnedMax_"\s*\+\s*(\w+)\)(?!\s*[,)])', r'nbt.getInteger(UpgradeKeys.kOwnedMax(\1))'),
        (r'nbt\.getInteger\("DamageCount_"\s*\+\s*(\w+)\)', r'nbt.getInteger(UpgradeKeys.kDamageCount(\1))'),
        (r'nbt\.getInteger\("TotalDamageCount_"\s*\+\s*(\w+)\)', r'nbt.getInteger(UpgradeKeys.kTotalDamageCount(\1))'),

        # Single-variant getBoolean patterns (simple, not in || chains)
        (r'nbt\.getBoolean\("HasUpgrade_"\s*\+\s*(\w+)\)(?!\s*\|\|)', r'nbt.getBoolean(UpgradeKeys.kHasUpgrade(\1))'),
        (r'nbt\.getBoolean\("WasPunished_"\s*\+\s*(\w+)\)', r'nbt.getBoolean(UpgradeKeys.kWasPunished(\1))'),

        # hasKey patterns (simple)
        (r'nbt\.hasKey\("upgrade_"\s*\+\s*(\w+)\)(?!\s*\|\|)', r'nbt.hasKey(UpgradeKeys.kUpgrade(\1))'),
        (r'nbt\.hasKey\("OriginalMax_"\s*\+\s*(\w+)\)', r'nbt.hasKey(UpgradeKeys.kOriginalMax(\1))'),
        (r'nbt\.hasKey\("OwnedMax_"\s*\+\s*(\w+)\)', r'nbt.hasKey(UpgradeKeys.kOwnedMax(\1))'),
    ]

    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)

    return content

def main():
    file_path = 'src/main/java/com/moremod/client/gui/MechanicalCoreGui.java'

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_lines = len(content.splitlines())

    # 执行替换
    new_content = refactor_gui_nbt(content)

    # 统计替换次数
    changes = sum(1 for old, new in zip(content.splitlines(), new_content.splitlines()) if old != new)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"✓ GUI 简单模式替换完成")
    print(f"  文件: {file_path}")
    print(f"  行数: {original_lines}")
    print(f"  修改行数: {changes}")
    print(f"")
    print(f"⚠ 注意：复杂的三变体模式需要手动重构")

if __name__ == '__main__':
    main()
