import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { createTypewriter } from './typewriter.js'
import { nightDayPlan, nightDayImages, nightDayTimes, nightDayImageWaitMs } from './nightDayPresentation.js'

// No game commands, storage writes or acknowledgement callbacks in this controller.
export function useNightDayPresentation(input) {
  const phase=ref('event'), text=ref(''), completeText=ref(false), diagnostic=ref(null)
  const active=shallowRef(null)
  const background=ref(nightDayImages.night)
  const done=new Set()
  let session='',timer,version=0,armed=false
  const writer=createTypewriter({publish:s=>{text.value=s.text;completeText.value=s.complete}})
  const cancel=()=>{++version;clearTimeout(timer);writer.dispose();armed=false}
  function finish() {
    if(active.value)done.add(active.value.key)
    cancel();active.value=null;phase.value='event';text.value='';completeText.value=false
  }
  function later(ms,action) {
    const token=version
    timer=setTimeout(()=>{if(token===version)action()},ms)
  }
  function typeDay() {
    phase.value='day-text';text.value='';completeText.value=false
    const all=active.value.dayText
    writer.reset(all,{instant:matchMedia('(prefers-reduced-motion: reduce)').matches})
  }
  function sceneReady() {
    if(!active.value || armed || !['night','dawn'].includes(phase.value))return false
    clearTimeout(timer);armed=true
    if(phase.value==='night')later(nightDayTimes.night,()=>{
      if(active.value.nightOnly)finish()
      else{background.value=nightDayImages.dawn;phase.value='dawn';armed=false;waitForImage()}
    })
    else later(nightDayTimes.dawn,typeDay)
    return true
  }
  function waitForImage() { later(nightDayImageWaitMs,()=>fail('NIGHT_IMAGE_TIMEOUT')) }
  function fail(code='NIGHT_IMAGE_UNAVAILABLE') {
    if(!active.value)return
    cancel();phase.value='error';diagnostic.value=code;completeText.value=true
  }
  function continueLocal() {
    if((phase.value==='day-text' && completeText.value) || phase.value==='error')finish()
  }
  watch(input,value=>{
    if(value.sessionId!==session){done.clear();session=value.sessionId;cancel();active.value=null;phase.value='event'}
    if(value.uncertain){cancel();active.value=null;phase.value='event';return}
    const plan=nightDayPlan(value.history,value.cursor,value.current,value.day)
    diagnostic.value=plan?.diagnostic || null
    const key=plan && !plan.diagnostic ? session+':'+plan.key : null
    if(key && active.value?.key===key)return
    if(!key || done.has(key)){
      if(active.value){cancel();active.value=null;phase.value='event'}
      return
    }
    cancel();active.value={...plan,key};background.value=nightDayImages.night;text.value='';completeText.value=false;phase.value='night';waitForImage()
  },{immediate:true})
  onBeforeUnmount(cancel)
  const artwork=computed(()=>active.value ? {mode:'scene',actors:[],diagnostic:null,
    background:background.value} : null)
  return {phase,artwork,text,completeText,diagnostic,sceneReady,continueLocal,fail}
}
