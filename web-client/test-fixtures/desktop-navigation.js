// Explicit navigation preconditions for business journeys after desktop menu parity.
// These helpers click real menus; they never invoke page.evaluate commands or APIs.
import {expect} from '@playwright/test'
export async function desktopHome(page){
 const window=page.getByTestId('desktop-vote-window');await expect(window).toBeVisible()
 if(await window.getAttribute('data-menu')==='home')return
 const back=window.getByRole('button',{name:'戻る',exact:true})
 if(await back.count())await back.click()
 else {await window.getByRole('button',{name:'指定投票',exact:true}).click();await window.getByRole('button',{name:'戻る',exact:true}).click()}
 await expect(window).toHaveAttribute('data-menu','home')
}
export async function desktopVote(page,method='FREE'){
 await desktopHome(page);await page.getByTestId('vote').click();await page.getByTestId('vote-'+method).click()
}
export async function desktopSelection(page,type='vote'){
 const mode=await page.getByTestId('desktop-vote-window').getAttribute('data-menu')
 if(mode===type)return
 await desktopHome(page);await page.getByRole('button',{name:'指示する',exact:true}).click()
 await page.getByRole('button',{name:'指定する',exact:true}).click()
 await page.getByRole('button',{name:({vote:'指定投票',divination:'占い指定',guard:'護衛指定'})[type],exact:true}).click()
}
export async function desktopClaims(page){await desktopHome(page);await page.getByRole('button',{name:'指示する',exact:true}).click();await page.getByRole('button',{name:'COを聞く',exact:true}).click()}
export async function desktopRecords(page,kind){await desktopHome(page);await page.getByRole('button',{name:'記録確認',exact:true}).click();await page.getByRole('button',{name:kind==='history'?'投票履歴':'疑い先',exact:true}).click()}
const completingReviews=new WeakSet()
export async function completeVoteReview(page){
 if(completingReviews.has(page))return
 completingReviews.add(page)
 try{for(let n=0;await page.getByTestId('desktop-vote-review').count();n++){
  if(n>=16)throw new Error('Vote review did not converge within16 authoritative rounds')
  await page.getByTestId('vote-review-continue').click()
 }}finally{completingReviews.delete(page)}
}
export async function desktopVoteConfirmation(page){
 const window=page.getByTestId('desktop-vote-window')
 if(await window.getAttribute('data-menu')!=='voteConfirm'){await desktopHome(page);await page.getByTestId('vote').click()}
 return page.getByTestId('vote-DESIGNATED')
}
export async function desktopSelectionIfAvailable(page){if(await page.getByTestId('desktop-vote-window').count())await desktopSelection(page,'vote')}
export async function expectNoEnabledVote(page){
 await expect(page.locator('[data-testid="vote-DESIGNATED"]:not(:disabled),[data-testid="vote-FREE"]:not(:disabled),[data-testid="vote-GREY"]:not(:disabled),.desktop-target:not(:disabled)')).toHaveCount(0)
}

export async function desktopClaimPlayers(page){
 await desktopClaims(page);await page.getByRole('button',{name:'プレイヤーのCOを聞く',exact:true}).click()
 return page.getByRole('region',{name:'CO確認対象',exact:true})
}
export async function desktopSkill(page,skill,actor){
 await desktopSelection(page,skill)
 const scene=page.getByRole('region',{name:skill==='divination'?'占い指定':'護衛指定',exact:true})
 const tools=scene.getByRole('button',{name:actor?actor+'の'+(skill==='divination'?'占い':'護衛')+'を指定':skill==='divination'?'全員の占いを指定':'全員の護衛を指定',exact:true})
 await tools.click()
 return scene
}
