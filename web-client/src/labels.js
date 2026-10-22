export const configurations = [
  ['jianyi', '简易', '从一场简洁的推理开始'],
  ['tongchang', '通常', '熟悉的经典村落'],
  ['yaoohu', '妖狐', '在第三阵营的迷雾中'],
  ['kuangxin', '狂信', '信任未必意味着同伴'],
  ['beide', '背德', '另一种忠诚，另一种结局'],
  ['maoyou', '猫又', '每次处刑都需更谨慎'],
  ['daxing', '大型', '更多发言，更深的线索'],
].map(([value, name, description]) => ({ value, name, description }))

export const roles = {
  NONE: '未 CO', zhan: '占い師', ling: '霊能者', lie: '狩人', gong: '共有者',
  mao: '猫又', wu: '村人', renlang: '人狼', kuangren: '狂人',
  kuangxinzhe: '狂信者', yaohu: '妖狐', beidezhe: '背徳者',
}
export const claimRoleNames = { zhan: '占い師', ling: '霊能者', lie: '狩人', gong: '共有', mao: '猫又' }
export const stages = {
  NIGHT_RESULT: '夜间结果', DAY_DIALOGUE: '白天发言', AFTERNOON_DIALOGUE: '回避发言',
  EXECUTION: '处刑', SETTLEMENT: '终局',
}
export const results = {
  VILLAGE_WIN: '村人阵营胜利', WOLF_WIN: '人狼阵营胜利', FOX_WIN: '妖狐胜利',
}
export const configurationName = value => configurations.find(item => item.value === value)?.name || value
