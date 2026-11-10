// Hierarchy only: displayed titles and bodies are read from the desktop Info files.
export function createHelpGroups(sources) {
  const read = name => {
    if (typeof sources[name] !== 'string') throw new Error('HELP_SOURCE_MISSING: ' + name)
    return sources[name].replaceAll('\r\n', '\n')
  }
  return [7, 6, 6, 8, 9].map((count, index) => {
    const id = String(index + 1)
    return {id, title: read('Info' + id + '.txt'), topics: Array.from({length: count}, (_, i) => {
      const topicId = id + '-' + (i + 1), source = 'Info' + topicId + '-1.txt'
      return {id: topicId, title: read('Info' + topicId + '.txt'), body: read(source), source, webNote: helpWebNotes[topicId] || null}
    })}
  })
}

// Browser-only differences, never part of or a replacement for desktop source text.
const helpWebNotes = {
  "1-5": "网页的公开信息仅使用服务器已公开的数据，不推算真实职业。浏览器诊断放在游戏窗口之外。",
  "1-6": "当前桌面“Continue”为未实现入口。网页仅支持同一标签页刷新后恢复临时会话；服务重启、会话过期或释放后不能续局，不是持久存档。",
  "1-7": "网页终局确认全部事件后，在 Replay 槽位右键并确认保存，不会自动保存录像。文件读入/写出用于主动交换；清除站点数据可能丢失本地录像。",
  "3-2": "在指定画面中选择对象，或拖动指定工具，再从“投票へ”确认。共有者回避仍然生效。自由投票和灰投使用服务器给出的候选。",
  "3-3": "拖动编号工具，或依次点击工具与对象设置预约。全体预约也会覆盖同一对象的个人预约；预约不保证实际执行结果。",
  "3-4": "拖动编号工具，或依次点击工具与对象设置预约。仅单列已经公开 CO 的行动者；全体预约会覆盖同一对象的个人预约，预约不保证执行结果。",
  "3-5": "网页只展示服务器已经公开的怀疑对象。未到可用日期时显示条件，不使用未公开信息补全。",
  "5-3": "网页日历使用服务器公开人数与绳数。刷新后若无法恢复投票前人数，只显示日期，不猜测人数。",
  "5-9": "原文中的历史版本修复记录是历史资料，不表示网页当前版本或功能保证。",
  "1-1": "网页实际胜负以服务器判定为准，不根据一般性说明自行推算 PP 或妖狐胜利条件。",
  "4-8": "包括 PP 在内的实际胜负以服务器判定为准，请确认终局结果，不根据这份原文自行重新判定。"
}

export const helpSourceHashes = {
  "Info1-1-1.txt": "58432e295ece558022e45737c1308f10400a22a61936586e33a4508b351c7bf4",
  "Info1-1.txt": "d6d4cbf3f31e9fb8ea31a474c9e2c70fdf6c907eabcdb45f9281137286de916d",
  "Info1-2-1.txt": "6fcead541d3eecf4f049b40fca6953057715a43eb1810e396ac0a06a8ff3bd09",
  "Info1-2.txt": "76fc7229c3f66b0989064fd953eabf9c4fc28e9b88d10141c2372ec552840826",
  "Info1-3-1.txt": "fa24344967f444e1e444b6e94752ce94b1ffedbd6b60a192527dcb43177c0665",
  "Info1-3.txt": "46f7eca10acb8157ae5f1f776a3e1a05cb4d2296850823a9d74c58150dff081f",
  "Info1-4-1.txt": "c69cd8ae1f246b79db856d6bf585cc0110c1ed98191e12f73cf92a17638babfb",
  "Info1-4.txt": "4f0027a873a1646c37aea01059d569915bbbb7c9c5ffad69ea42b39004b6a7cd",
  "Info1-5-1.txt": "704f0b8065ba6c307374393acaeba45426c1c15b996232fcec7b06b14b5c5de8",
  "Info1-5.txt": "cf0ec6022114c4533abb4bf5b715e3ecfa787092d78e13afcb6b5e3b33ba8ff3",
  "Info1-6-1.txt": "f889f279a8de724fdfff896c3d9e602749570f0f423b8af76132418f8d222594",
  "Info1-6.txt": "a3030bf8f16dc63c6b277564c68f78a3be5abe5f2907c51826dee7184ff793fd",
  "Info1-7-1.txt": "6b7cc9ba5c4934980678ceb3fff957f259a78186de9104adaf404f8646609505",
  "Info1-7.txt": "d4c98240597ead46190ace240228154a97ccd761d96bb21fa726ad0f12fd1016",
  "Info1.txt": "a7f1945290bbf59dcc6da88f36b200c1567b1fe110529bc180ee9758639f9a48",
  "Info2-1-1.txt": "b850f48623868c1d2ea7faa9fdedb08e3446307e52ae700f2cc34bdc0a004ce0",
  "Info2-1.txt": "4781f6b2a1586eeeda92722c09b2c9885d0f2c09e0a4de30b72f74c04b84286b",
  "Info2-2-1.txt": "71fcc160614598bffdf7b6cb67234297fc4431a807ae9a1ce2f1c561f284bd35",
  "Info2-2.txt": "041a9766666be2a509cec458fe83d8d5a6a61d9b7c0fc2e39842dadf3759e3f4",
  "Info2-3-1.txt": "429adf1d3197f08ac19b34b78213372d74edee5142cd4856be06443757c6ea40",
  "Info2-3.txt": "88eed9bc795ae92b160fb5e8130bce88c23f1409b61d89b44e237453fee9c52b",
  "Info2-4-1.txt": "0443cb39f1feb836d1c148924f08764f1edaf1d04cfb79cddbb6e048dedf61d4",
  "Info2-4.txt": "dc00f6cd2d59af270b08137bfa46788ae3ecb91134a6fc26233945d94f9e3230",
  "Info2-5-1.txt": "c7522fe7e208b869ea6add04032da9f363baee5b745578ec8ed64208ae81f353",
  "Info2-5.txt": "02ca6a47a3211617ff17dc1840b502e69f42076bbe5aab5268c9e655e0e154ee",
  "Info2-6-1.txt": "80f589ea9ef64a093141a1477be414c415108126f45c3b6c40748c7cd578bd79",
  "Info2-6.txt": "b5ab1964a7653ba7a9147f3c2ab17c0eddf0b6e44531fee4709aa256e2edbf3e",
  "Info2.txt": "bc3c0420e12ec7d8cd26739586bcbf6eb2ac2c5e575f8a66b7b50888d9882d0b",
  "Info3-1-1.txt": "c3e2123ce0db51177867ac991df1521ffa7735c2cc58c9861b4e950e9da1a050",
  "Info3-1.txt": "e8c385a819027a0ed6a67669b48ee59a2f6a3bae484cd41dea4b81d429133848",
  "Info3-2-1.txt": "1eda6724fbccfead3fabb3f347c8635c63750a75d40788f414715e00a64c113e",
  "Info3-2.txt": "85a386e60e94e32ea83e69c4924e83a7f2450a458f1f139972256f537383cec9",
  "Info3-3-1.txt": "368e86754019af92a5de7af2bb26fc05c35dcb88ec263b63983bfd3db874d7f4",
  "Info3-3.txt": "033f34fb76cfd1c1a924db5a53c168bdb146e11e951adf75b4b9d0de7000cb7b",
  "Info3-4-1.txt": "c132978eb5ef15aaeb802886d20bbd133045d5d20680d04cd7ab5b73e1b3ba0b",
  "Info3-4.txt": "338a938fb21881b5f4f66742ac1487fb42e7f422bd8e0f72832751013651a4da",
  "Info3-5-1.txt": "aaafb2c304ab527a07080a0199fe82675e0c8dc8c25d3e3342b0c7641446918d",
  "Info3-5.txt": "b2e38a3802d513661d343fba24a96a43c65e3d5db1488ea8799caf1bf6b7c399",
  "Info3-6-1.txt": "b80ae8b157441d2c078d858cb4857df1766bc2ce9ba29454904a2f95f6905d9f",
  "Info3-6.txt": "7c68e92610582eca8dc422ea71c691c7b744ce41a7b02aa291a737254a25c3d9",
  "Info3.txt": "c59d05161c59d629972994dda7345956edc6aca5836570a3fbe311891bcb6082",
  "Info4-1-1.txt": "c7828a798d6589b6d2d6db4bbc3067e5002ae5dfa45f6048404f09b969bb11de",
  "Info4-1.txt": "fc8842c641d9150647f928e6c294ce26dd51ecae5ba30b114a78ddedcd398815",
  "Info4-2-1.txt": "b3336b8c85f5bc55f5b7f06fe38083b04695e06b7e2437f78a8640fd78c902a8",
  "Info4-2.txt": "be80834b9aa7dee407bac8e10280c131ec09737a4466a7e478812eeef21885fe",
  "Info4-3-1.txt": "b9e119e2abf6e8619c69a49d4e60b5fa3578281fbb3577e5647db5df11f1d5cd",
  "Info4-3.txt": "857f05d898603d9193a9623d3899b2e8dc6627f9d3e8048de7edb7a53c7e755c",
  "Info4-4-1.txt": "9c6629b77f29cdc841d8ada8eb361d60ca8d1b943f72d46fa89e805650d40dc1",
  "Info4-4.txt": "1d25f33b5efdad1234b139ec16828454144ba7c09467c8c32936dae1641a8b6e",
  "Info4-5-1.txt": "9d45fd0dd1926d36cd5dcb26f7075e3a321f0f7376a3297c185bbd0e8432272a",
  "Info4-5.txt": "c07255f6e96bcff7c484e6d69fcd9c8a53f2ac51229170516e91a428c62f4e6e",
  "Info4-6-1.txt": "29bc6c8d91f741243bd63dba47ce532b0764671b518b2b0a5af1dfb5af16c697",
  "Info4-6.txt": "fbe190a2bdc4efd716b22d5aced1d15dba77d0c06776053724a3a6dc9f9ded13",
  "Info4-7-1.txt": "7f7fa82047b9acc4c3a43f549854bcb5243365da3cd3c4a4a70969b70637903c",
  "Info4-7.txt": "f6bcdbbbb4e20839df9125d2c7d3b40d98661014a7719d150664b69756f9920f",
  "Info4-8-1.txt": "df20664c543f7cef78cb39f4b361e25b8a44ae654c320b8a5104a063d2f96c3a",
  "Info4-8.txt": "6e0dcf0f015a965d2cfb74bc1837354200b43498c60c68f85b39bdf33bbaace5",
  "Info4.txt": "d8abd16478124d67fe4dca7edc9ddc015fd59b0aec5f8c543fc2b649bfe9c9db",
  "Info5-1-1.txt": "0769c4c0a2a8c531d2f63c53c1b74ec72cf483d7b10c3b0f133cdfc81c55b1a9",
  "Info5-1.txt": "54943164eccfcf1bd7bd8d8a098e951f24655ca0b95807f3f89c821d838d1ee5",
  "Info5-2-1.txt": "7de688690d663edadb5d2dd597c544d71c44292b4df74e2e725a0c7e29001f3b",
  "Info5-2.txt": "a49a1a7525d7bb6dcf5d3a00728d175b7e742d1f8232d95a520a8d0efa830d01",
  "Info5-3-1.txt": "204b1a3945bfcd3abff34a233c6301f903287854d0c74a38be02978aaef06089",
  "Info5-3.txt": "c6a2abdf63035fa400a7658866cb4780cf4f799b2183611ae8fb848223f35768",
  "Info5-4-1.txt": "f589e8b4b574b7c424bc388f4a0c2efd1e505f703254457941b42c9419ad5f9a",
  "Info5-4.txt": "0f6c9c69e6e8e95d0717e4c7bb8afb55f60dce7e7f436c733c709f682f7f8e0e",
  "Info5-5-1.txt": "d647a2a448372701b61eb83f6e116e2b3694b9501ce6ac4817424f5fe43b0762",
  "Info5-5.txt": "8187c9f86b1ec5555cf4a6e4a367efed5ea98b060b5335e9771bc89ba1ca59ca",
  "Info5-6-1.txt": "f1aa528bba9ef4777834a36caf12ad661ded658475b9764b698eb011f89d4295",
  "Info5-6.txt": "7c68e92610582eca8dc422ea71c691c7b744ce41a7b02aa291a737254a25c3d9",
  "Info5-7-1.txt": "13ce1f28ed367312f12ab82287e29934e0ea15546419da821d14f184116619db",
  "Info5-7.txt": "55ba17e1ba67c112d765cd1811e061ae4effc25551771bd41e26ece5f5fa90ce",
  "Info5-8-1.txt": "ac894ce530f71c012176700bbd09349f2616f55d7da07ddf9a16eec1bf3afa86",
  "Info5-8.txt": "be42bae6d252a6c51a057ef7552c3a12fb9b1d2b6b5be070688838f595c65ce6",
  "Info5-9-1.txt": "6c861407499e4fb2bba5ddd6dc156c81b20011c6943f2cf627351244a3757088",
  "Info5-9.txt": "445905122e87b9907cf5f8334a56283e2ce8943f6455a8f8042163ce35f81ab5",
  "Info5.txt": "d0bc536045e925e7d88954d4a17ddcf77fa8349cdcd4db97b4dadfe889dd1dff"
}
