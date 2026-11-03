// Explicit graphical-launch precondition for existing business journeys.
// Navigation never creates a session; only the chosen source setup button does.
const choices = new WeakMap()
const ids = {'簡易':'jianyi','通常':'tongchang','妖狐':'yaoohu','狂信':'kuangxin','背徳':'beide','猫又':'maoyou','大型':'daxing'}
export async function chooseDesktopConfiguration(page, configuration) {
  choices.set(page, ids[configuration] || configuration)
  if (await page.getByTestId('desktop-title').isVisible()) await page.getByTestId('title-start').click()
}
export async function startDesktopGame(page, configuration) {
  await chooseDesktopConfiguration(page, configuration || choices.get(page) || 'jianyi')
  await page.getByTestId('setup-' + choices.get(page)).click()
}
