import {test,expect} from '@playwright/test'
import {publicSnapshot} from '../test-fixtures/public-players.js'

test('vote result keeps the already displayed day and public roster until execution is continued',async({page})=>{
 const id='00000000-0000-4000-8000-000000000053',before=publicSnapshot()
 before.view.gameDay=3;before.legalVoteActions=[{method:'FREE',candidates:[1,2]}]
 const vote={day:3,method:'FREE',candidates:[1,2],rounds:[{round:1,maximumVotes:4,leaders:[1],lines:[{playerNumber:1,targetPlayerNumber:2,receivedVotes:4},{playerNumber:2,targetPlayerNumber:1,receivedVotes:0}]}]}
 const event={eventname:'cxs',ch1:'Abel',ch2:'Erich',ch3:'NONE'}
 const after={...before,revision:2,view:{...before.view,gameDay:4,aliveCount:43,deathCount:1,players:before.view.players.map((p,i)=>i===0?{...p,alive:false,deathDay:3,deathReason:'EXECUTION'}:p)},voteHistory:[vote],events:[event],presentedEvents:[{stage:'EXECUTION',event}],nextEventCursor:1}
 await page.route('**/api/v1/games',r=>r.fulfill({status:201,json:{sessionId:id,update:before}}))
 await page.route('**/api/v1/games/'+id+'/votes',r=>r.fulfill({json:{voteCompleted:true,update:after}}))
 await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
 await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
 await page.getByTestId('vote').click();await page.getByTestId('vote-FREE').click()
 const review=page.getByTestId('desktop-vote-review')
 await expect(review).toBeVisible()
 await expect(review.locator('.desktop-calendar')).toContainText('3日目')
 await expect(review.locator('.desktop-calendar')).toContainText('生存者:44')
 await expect(review.locator('.desktop-roster-row').first().locator('.death-marker')).toHaveCount(0)
 await expect(page.getByTestId('desktop-message')).toHaveCount(0)
 await expect(page.getByTestId('settlement-board')).toHaveCount(0)
 const stored=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1')),writes=[]
 page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 await page.getByTestId('vote-review-continue').click();await expect(page.getByTestId('next-event')).toBeVisible()
 expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(stored)
 expect(writes).toEqual([])
})
