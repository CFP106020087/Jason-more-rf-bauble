#!/usr/bin/env python3
"""
精确替换 NBT 键名为 UpgradeKeys 方法调用
"""
import re

def refactor_nbt_keys(content):
    """替换硬编码的 NBT 键名"""
    replacements = [
        # 惩罚系统键（需要参数）
        (r'"PenaltyExpire_"\s*\+\s*(\w+)', r'UpgradeKeys.kPenaltyExpire(\1)'),
        (r'"PenaltyCap_"\s*\+\s*(\w+)', r'UpgradeKeys.kPenaltyCap(\1)'),
        (r'"PenaltyTier_"\s*\+\s*(\w+)', r'UpgradeKeys.kPenaltyTier(\1)'),
        (r'"PenaltyDebtFE_"\s*\+\s*(\w+)', r'UpgradeKeys.kPenaltyDebtFE(\1)'),
        (r'"PenaltyDebtXP_"\s*\+\s*(\w+)', r'UpgradeKeys.kPenaltyDebtXP(\1)'),

        # 状态标记键
        (r'"Disabled_"\s*\+\s*(\w+)', r'UpgradeKeys.kDisabled(\1)'),
        (r'"IsPaused_"\s*\+\s*(\w+)', r'UpgradeKeys.kPaused(\1)'),
        (r'"HasUpgrade_"\s*\+\s*(\w+)', r'UpgradeKeys.kHasUpgrade(\1)'),
        (r'"OwnedMax_"\s*\+\s*(\w+)', r'UpgradeKeys.kOwnedMax(\1)'),

        # upgrade_ 特殊处理（只替换明显的模式）
        (r'"upgrade_"\s*\+\s*upgradeId', r'UpgradeKeys.kUpgrade(upgradeId)'),
        (r'"upgrade_"\s*\+\s*id\b', r'UpgradeKeys.kUpgrade(id)'),
    ]

    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)

    return content

def main():
    file_path = 'src/main/java/com/moremod/item/ItemMechanicalCore.java'

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_lines = len(content.splitlines())

    # 执行替换
    new_content = refactor_nbt_keys(content)

    # 统计替换次数
    changes = sum(1 for old, new in zip(content.splitlines(), new_content.splitlines()) if old != new)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"✓ 替换完成")
    print(f"  文件: {file_path}")
    print(f"  行数: {original_lines}")
    print(f"  修改行数: {changes}")

if __name__ == '__main__':
    main()
