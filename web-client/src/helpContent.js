// WEB-029 Japanese summaries of the existing Chinese help, not recovered Japanese originals.
export const helpGroups = [
  {
    "id": "1",
    "title": "電脳村について",
    "topics": [
      {
        "id": "1-1",
        "title": "人狼ゲームとは",
        "body": "村に紛れた人狼を、昼の議論と投票で探すゲームです。夜には人狼の襲撃があり、能力者が行動します。勝利条件や特殊役職は配役によって異なります。",
        "webNote": "Web版では勝敗はサーバーの判定に従います。一般的な人狼の説明だけで独自のPPや妖狐の条件を判断しないでください。",
        "source": "Info1-1-1.txt"
      },
      {
        "id": "1-2",
        "title": "電脳村の特徴",
        "body": "登場人物は全員NPCです。発言を読み、投票やCO確認、占い・護衛の予約を指示できます。指示しない場合は、それぞれの判断で行動します。",
        "webNote": null,
        "source": "Info1-2-1.txt"
      },
      {
        "id": "1-3",
        "title": "配役",
        "body": "役職は対局ごとに割り振られます。七つの配役の人数と役職構成は「配役説明」で確認できます。人物の見た目から今回の役職は分かりません。",
        "webNote": null,
        "source": "Info1-3-1.txt"
      },
      {
        "id": "1-4",
        "title": "一日の流れ",
        "body": "昼は議論と投票・処刑、夜は襲撃と能力者の行動が行われます。護衛や特殊役職のため、死亡人数が常に二人になるとは限りません。",
        "webNote": null,
        "source": "Info1-4-1.txt"
      },
      {
        "id": "1-5",
        "title": "公開情報のまとめ",
        "body": "CO、公開された能力報告、死亡履歴や投票履歴を振り返れます。COと報告は本人の発言であり、真の役職を保証しません。未公開の役職は表示しません。",
        "webNote": "Web版では公開情報パネル・投票履歴を利用します。デスクトップの色や画面配置をそのまま再現した説明ではありません。",
        "source": "Info1-5-1.txt"
      },
      {
        "id": "1-6",
        "title": "保存と再開",
        "body": "デスクトップ版の説明には自動保存とタイトル画面からの再開があります。Web版の保存範囲は下の補足を確認してください。",
        "webNote": "Web版は同じタブの再読み込みで未確認イベントから再開できます。保存するのは会話カーソルと一時セッションIDです。サービス再起動・セッション解放後は再開できません。永続セーブ機能ではありません。",
        "source": "Info1-6-1.txt"
      },
      {
        "id": "1-7",
        "title": "過去の村の記録",
        "body": "デスクトップ版には対局のリプレイと保存機能があります。役職や特殊死因を色分けして振り返る説明が原文にあります。Web版の対応状況は下の補足を確認してください。",
        "webNote": "Web版のReplay閲覧・ファイル交換は未実装です。ここではデスクトップ版の説明を資料として掲載しています。",
        "source": "Info1-7-1.txt"
      }
    ]
  },
  {
    "id": "2",
    "title": "役職",
    "topics": [
      {
        "id": "2-1",
        "title": "村人",
        "body": "特別な能力はありません。議論、公開情報、投票を手掛かりに人狼を探します。",
        "webNote": null,
        "source": "Info2-1-1.txt"
      },
      {
        "id": "2-2",
        "title": "占い師",
        "body": "夜に一人を占い、人狼なら黒●、それ以外なら白○と判定します。白判定が村人陣営を保証するわけではありません。",
        "webNote": null,
        "source": "Info2-2-1.txt"
      },
      {
        "id": "2-3",
        "title": "霊能者",
        "body": "処刑された人物が人狼かを調べます。人狼なら黒●、それ以外なら白○です。",
        "webNote": null,
        "source": "Info2-3-1.txt"
      },
      {
        "id": "2-4",
        "title": "狩人",
        "body": "夜に一人を護衛し、人狼の襲撃から守ります。自分自身は護衛できません。呪殺、猫の呪い、後追いを防ぐ能力ではありません。",
        "webNote": null,
        "source": "Info2-4-1.txt"
      },
      {
        "id": "2-5",
        "title": "狂人",
        "body": "人間でありながら人狼陣営の勝利を目指す役職です。特別な能力はなく、人狼の会話も聞けません。原文末尾の判定に関する表現は曖昧なため、Web版では白判定と陣営を区別して読んでください。",
        "webNote": null,
        "source": "Info2-5-1.txt"
      },
      {
        "id": "2-6",
        "title": "人狼",
        "body": "夜に村人を襲撃します。人狼同士は仲間を知り、村人には分からない会話で連絡します。",
        "webNote": null,
        "source": "Info2-6-1.txt"
      }
    ]
  },
  {
    "id": "3",
    "title": "指示・記録",
    "topics": [
      {
        "id": "3-1",
        "title": "CO確認",
        "body": "潜伏している能力者に役職の公開（CO）を求めます。本物だけでなく、人外の騙りが出る可能性もあります。Web版では役職別・プレイヤー別に確認できます。",
        "webNote": null,
        "source": "Info3-1-1.txt"
      },
      {
        "id": "3-2",
        "title": "指定投票",
        "body": "一人または複数人を候補に指定します。候補が複数なら、それぞれの判断で投票先を選びます。指定対象がCOして投票を回避する場合があります。",
        "webNote": "Web版は候補をチェックし「投票を確定」を押します。共有者の回避は無効化されません。自由・グレー投票はサーバーが提示する候補で行います。",
        "source": "Info3-2-1.txt"
      },
      {
        "id": "3-3",
        "title": "占い予約",
        "body": "占い先の候補を一人または複数人予約します。複数ならその中から選びます。原文のドラッグ操作はデスクトップ版向けです。",
        "webNote": "Web版はチェック式の予約です。全員の予約操作は、その対象の個別予約も上書きします。予約は結果の保証ではありません。",
        "source": "Info3-3-1.txt"
      },
      {
        "id": "3-4",
        "title": "護衛予約",
        "body": "護衛先の候補を予約します。潜伏候補への全体予約と、公開COした行動者ごとの予約があります。原文のドラッグ操作はデスクトップ版向けです。",
        "webNote": "Web版はチェック式の予約です。公開COの行動者だけを個別表示します。全体操作は同じ対象の個別予約も上書きし、予約は結果を保証しません。",
        "source": "Info3-4-1.txt"
      },
      {
        "id": "3-5",
        "title": "疑い先",
        "body": "各人物が誰を疑っているかを確認します。GS（グレースケール）は疑いの強さを並べたもので、真の役職の一覧ではありません。",
        "webNote": "Web版ではサーバーが公開した疑い先だけを表示します。利用できない日には利用条件を表示し、未公開情報で補完しません。",
        "source": "Info3-5-1.txt"
      },
      {
        "id": "3-6",
        "title": "投票履歴",
        "body": "過去の投票先を確認します。人狼が仲間に投票する利点や不利を考えると、推理の材料になります。",
        "webNote": null,
        "source": "Info3-6-1.txt"
      }
    ]
  },
  {
    "id": "4",
    "title": "用語",
    "topics": [
      {
        "id": "4-1",
        "title": "CO",
        "body": "Coming Outの略。自分の役職を村に公表することです。占いCO、霊能COなどと呼びます。",
        "webNote": null,
        "source": "Info4-1-1.txt"
      },
      {
        "id": "4-2",
        "title": "白・黒・グレー",
        "body": "占いや霊能の人狼判定を黒●、それ以外を白○と呼びます。未判定者をグレーと呼び、通常はCO者を除きます。グレーから疑う人物に投票することをグレランと呼びます。",
        "webNote": null,
        "source": "Info4-2-1.txt"
      },
      {
        "id": "4-3",
        "title": "黒特攻",
        "body": "偽占い師が初日に黒を出すこと。狂人が人狼に自分の存在を知らせる狙いなどがありますが、霊能とのライン切れや共有トラップの危険もあります。狼特攻・狐特攻という呼び方もあります。",
        "webNote": null,
        "source": "Info4-3-1.txt"
      },
      {
        "id": "4-4",
        "title": "誤爆",
        "body": "占い師を騙る狂人が、仲間を知らずに人狼へ黒を出すことです。人狼側も狂人を知らずに襲うことがあり、分かったうえで議論を乱すために襲う場合もあります。",
        "webNote": null,
        "source": "Info4-4-1.txt"
      },
      {
        "id": "4-5",
        "title": "共有トラップ",
        "body": "偽占い師が潜伏共有者に黒を出すこと。共有者同士は互いを知っているため、その占い師の偽が判明します。",
        "webNote": null,
        "source": "Info4-5-1.txt"
      },
      {
        "id": "4-6",
        "title": "吊り回避",
        "body": "指定投票で処刑されそうな潜伏能力者が、投票前にCOして回避を図ること。狩人は不在を人狼に知らせないため、COせず処刑される場合もあります。",
        "webNote": null,
        "source": "Info4-6-1.txt"
      },
      {
        "id": "4-7",
        "title": "GJ",
        "body": "Good Jobの略で、主に狩人の護衛成功を指します。ただし狩人自身に成功が確定するわけではありません。死体なしには護衛成功と妖狐襲撃などの可能性があります。",
        "webNote": null,
        "source": "Info4-7-1.txt"
      },
      {
        "id": "4-8",
        "title": "PP",
        "body": "Power Play。狂人と人狼の票が村側に拮抗する状況です。原文は本アプリ独自の勝敗扱いと、実際の対人戦でのRPPの違いを説明しています。これは一般の人狼すべてに共通する勝敗規則ではありません。",
        "webNote": null,
        "source": "Info4-8-1.txt"
      }
    ]
  },
  {
    "id": "5",
    "title": "戦略",
    "topics": [
      {
        "id": "5-1",
        "title": "ライン確認",
        "body": "占い師の黒を処刑し、霊能結果と照らし合わせて真偽を考えます。霊能が複数いる場合は結果も割れ得るので、情報の確かさは下がります。",
        "webNote": null,
        "source": "Info5-1-1.txt"
      },
      {
        "id": "5-2",
        "title": "ローラー",
        "body": "同じ役職のCO者を順に処刑して、人外を確実に減らす考え方です。霊能ローラーで狩人の居場所を広く保つ利点などがある一方、指定投票が続くと投票履歴の推理材料が減ります。",
        "webNote": null,
        "source": "Info5-2-1.txt"
      },
      {
        "id": "5-3",
        "title": "吊り縄計算",
        "body": "残る処刑回数の中で人外を処刑できるか考えます。GJや妖狐襲撃で回数が増えることもあります。原文の例は妖狐村の二日目、15人・7縄に対し人外5人なので村側を吊れる余裕が2縄という説明です。",
        "webNote": "Web版の現状はデスクトップ版の右上の縄数表示と同じ画面配置ではありません。",
        "source": "Info5-3-1.txt"
      },
      {
        "id": "5-4",
        "title": "安定進行",
        "body": "各占い師の視点で人外候補を整理し、安全に最終日へ進むことを考えます。全人外を見つけた場合や、残る候補が縄数より少ない場合など、「仕事終了」となる条件を比較します。",
        "webNote": null,
        "source": "Info5-4-1.txt"
      },
      {
        "id": "5-5",
        "title": "決め打ち",
        "body": "占い師の真を一人に決めるのは最後の手段です。終盤までに集まった投票履歴などを比べ、仲間への投票がどの状況で起きたのかも考えます。",
        "webNote": null,
        "source": "Info5-5-1.txt"
      },
      {
        "id": "5-6",
        "title": "投票履歴の読み方",
        "body": "人狼は仲間を知っている点が村人と異なります。仲間に投票しにくい状況や、逆に仲間を切らざるを得ない状況を考えることが、履歴から得られる手掛かりになります。",
        "webNote": null,
        "source": "Info5-6-1.txt"
      },
      {
        "id": "5-7",
        "title": "襲撃考察1",
        "body": "占い師への襲撃が護衛されると、同じ場所を再び襲いにくくなります。GJ後に狩人を探して襲った結果、また死体なしになったら、人狼はどう動くでしょうか。原文は本アプリに「告発」がない点にも触れています。",
        "webNote": null,
        "source": "Info5-7-1.txt"
      },
      {
        "id": "5-8",
        "title": "襲撃考察2",
        "body": "占い師を襲って護衛されると、後の護衛報告でその占い師の信用が上がる危険があります。占い師を襲わず詰みを避ける戦術や、一見詰みに近づく襲撃にどんな意図があるかを考えます。",
        "webNote": null,
        "source": "Info5-8-1.txt"
      },
      {
        "id": "5-9",
        "title": "おわりに・更新履歴",
        "body": "原文は、対人の人狼には本作で表現できない展開があること、興味があればさらに研究してほしいことを述べています。続いて重制版の開発者と2025年12月〜2026年6月の更新履歴があります。原文全文は下から閲覧できます。この要約は原作者の新しい発言ではありません。",
        "webNote": "原文の過去バージョンに関する修正記録は履歴資料です。Web版の動作保証・最新バージョンの表明ではありません。",
        "source": "Info5-9-1.txt"
      }
    ]
  }
]

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
