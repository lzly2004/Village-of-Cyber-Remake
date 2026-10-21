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
  NONE: '未 CO', zhan: '占卜师', ling: '灵媒师', lie: '猎人', gong: '共有者',
  mao: '猫又', wu: '村人', renlang: '人狼', kuangren: '狂人',
  kuangxinzhe: '狂信者', yaohu: '妖狐', beidezhe: '背德者',
}
export const stages = {
  NIGHT_RESULT: '夜间结果', DAY_DIALOGUE: '白天发言', AFTERNOON_DIALOGUE: '回避发言',
  EXECUTION: '处刑', SETTLEMENT: '终局',
}
export const results = {
  VILLAGE_WIN: '村人阵营胜利', WOLF_WIN: '人狼阵营胜利', FOX_WIN: '妖狐胜利',
}
const eventLabels = {
  NONE: '事件', gyfo1r: '确认共有同伴', gyfo1: '共有者公开同伴', gyho2: '共有者 CO',
  qfgsw3: '报告潜伏共有者死亡', gkgsw4: '报告共有者死亡', qfjcqr5r: '确认潜伏解除',
  qfjc5: '解除潜伏', gycx6: '报告共有者被处刑', zco7: '占卜师 CO',
  jhdh8b: '回应占卜黑结果', zjgh8b: '公布占卜黑结果', jbdh8r: '回应占卜白结果',
  zjgb8: '公布占卜白结果', zrzbjg9: '补充昨日占卜结果', zbdxsw10: '报告占卜对象死亡',
  gprz11p: '确认共有者身份', gprz11r: '回应共有者认证', zcrh12r: '回应初日黑结果',
  zcrh12: '公布初日黑结果', gyzs13: '共有者发出指示', zs14: '报告咒杀',
  zspz15: '咒杀破绽', szsm16: '讨论咒杀可能', wz17: '占卜完成', lnco18: '灵媒师 CO',
  ljgh19b: '公布灵媒黑结果', ljgb19: '公布灵媒白结果', cxs: '处刑发言',
  crsl: '村人胜利发言', rlsl: '人狼胜利发言', yhsl: '妖狐胜利发言', krsl: '狂人胜利发言',
  hblr: '猎人回避', hbg: '共有者回避', hbln: '灵媒师回避', hbz: '占卜师回避',
  hbm: '猫又回避', lrco: '猎人 CO', mco: '猫又 CO',
  yjsw: '夜间死亡', hzsw: '后追死亡', mzsw: '猫咒死亡', wsw: '平安夜，无人死亡',
}
export const eventLabel = event => eventLabels[event.eventname] || event.eventname
export const configurationName = value => configurations.find(item => item.value === value)?.name || value
