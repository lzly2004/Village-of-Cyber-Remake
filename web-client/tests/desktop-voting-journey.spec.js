import {test,expect} from '../test-fixtures/presentation-test.js'
import {desktopVote,desktopClaims} from '../test-fixtures/desktop-navigation.js'
import {drainEvents} from './drain-events.js'
test('real complete game uses desktop menus, CO inquiry and authoritative vote review without lost events',async({page,request})=>{
 await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.getByTestId('next-event')).toBeVisible()
 const id=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).sessionId)
 try{
  await drainEvents(page);await desktopClaims(page);await page.getByRole('button',{name:'占い師のCOを聞く',exact:true}).click();await drainEvents(page)
  let votes=0
  while(!(await page.locator('.settlement-board').count())){
   expect(votes++).toBeLessThan(40);await desktopVote(page,'FREE');await drainEvents(page)
  }
  const snapshot=await(await request.get('/api/v1/games/'+id+'?eventCursor=0')).json()
  expect(snapshot.phase).toBe('FINISHED');expect(snapshot.voteHistory.length).toBeGreaterThan(0)
  const state=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')))
  expect(state.eventCursor).toBe(snapshot.nextEventCursor)
  await expect(page.locator('.settlement-card')).toHaveCount(snapshot.view.players.length)
  await expect(page.locator('.settlement-board')).toContainText('真の職業')
 }finally{await request.delete('/api/v1/games/'+id)}
})
