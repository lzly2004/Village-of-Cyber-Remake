// Names checked against images/game1..7.png; role counts mirror GameStatus.startGame.
export const configurations = [
  {
    "value": "jianyi",
    "name": "簡易",
    "description": "12人・基本の配役",
    "playerCount": 12,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "wu": 6,
      "renlang": 2,
      "kuangren": 1
    },
    "sourceImage": "images/game1.png"
  },
  {
    "value": "tongchang",
    "name": "通常",
    "description": "16人・共有者のいる村",
    "playerCount": 16,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "gong": 2,
      "wu": 7,
      "renlang": 3,
      "kuangren": 1
    },
    "sourceImage": "images/game2.png"
  },
  {
    "value": "yaoohu",
    "name": "妖狐",
    "description": "16人・妖狐のいる村",
    "playerCount": 16,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "gong": 2,
      "wu": 6,
      "renlang": 3,
      "kuangren": 1,
      "yaohu": 1
    },
    "sourceImage": "images/game3.png"
  },
  {
    "value": "kuangxin",
    "name": "狂信",
    "description": "17人・狂信者のいる村",
    "playerCount": 17,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "gong": 2,
      "wu": 7,
      "renlang": 3,
      "kuangxinzhe": 1,
      "yaohu": 1
    },
    "sourceImage": "images/game4.png"
  },
  {
    "value": "beide",
    "name": "背徳",
    "description": "18人・背徳者のいる村",
    "playerCount": 18,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "gong": 2,
      "wu": 7,
      "renlang": 3,
      "kuangren": 1,
      "yaohu": 1,
      "beidezhe": 1
    },
    "sourceImage": "images/game5.png"
  },
  {
    "value": "maoyou",
    "name": "猫又",
    "description": "18人・猫又のいる村",
    "playerCount": 18,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "gong": 2,
      "mao": 1,
      "wu": 6,
      "renlang": 4,
      "kuangren": 1,
      "yaohu": 1
    },
    "sourceImage": "images/game6.png"
  },
  {
    "value": "daxing",
    "name": "大型",
    "description": "20人・特殊役職を含む村",
    "playerCount": 20,
    "roleCounts": {
      "zhan": 1,
      "ling": 1,
      "lie": 1,
      "gong": 2,
      "mao": 1,
      "wu": 7,
      "renlang": 4,
      "kuangxinzhe": 1,
      "yaohu": 1,
      "beidezhe": 1
    },
    "sourceImage": "images/game7.png"
  }
]

export const roles = {
  NONE: '未CO', zhan: '占い師', ling: '霊能者', lie: '狩人', gong: '共有者',
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
