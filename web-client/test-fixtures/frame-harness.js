// Browser-only test harness, not imported by the production entry.
import {createApp,h} from 'vue'
import GameStage from '../src/GameStage.vue'
export function mountFrame(){
 const host=document.createElement('div');host.id='frame-probe';host.style.width='1000px';document.body.append(host);window.frameReads=0
 createApp({setup(){return()=>h(GameStage,{reading:true,message:true,onReadContinue:()=>window.frameReads++},{default:()=>h('p','ここを二度押すと次の文章へ進みます。')})}}).mount(host)
}
