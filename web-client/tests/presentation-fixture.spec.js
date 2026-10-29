import {test,expect} from '../test-fixtures/presentation-test.js'
import {publicSnapshot} from '../test-fixtures/public-players.js'

// Reproduce CI's cross-command clock race without real sleep or relaxed timeout:
// a timestamp sampled by one command may already be past at pauseAt's arrival.
test('local presentation journey tolerates time elapsed between clock commands without acknowledging an event',async({page})=>{
 const pause=page.clock.pauseAt.bind(page.clock)
 page.clock.pauseAt=async time=>{await page.clock.runFor(100);return pause(time)}
 const snapshot=publicSnapshot(),event={eventname:'wsw',ch1:'NONE',ch2:'NONE',ch3:'NONE'}
 snapshot.events=[event];snapshot.presentedEvents=[{stage:'NIGHT_RESULT',event}];snapshot.nextEventCursor=1
 const id='00000000-0000-4000-8000-000000000052'
 await page.route('**/api/v1/games',r=>r.fulfill({status:201,json:{sessionId:id,update:snapshot}}))
 await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
 await expect(page.getByTestId('next-event')).toBeEnabled()
 expect(await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).eventCursor)).toBe(0)
 await expect(page.getByTestId('event-dialogue')).toContainText('犠牲者はいませんでした。')
})
