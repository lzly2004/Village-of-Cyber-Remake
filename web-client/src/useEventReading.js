import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { eventDialogue } from './eventDialogue.js'
import { createTypewriter } from './typewriter.js'

// No command callbacks, fetch, storage or private game fields in this controller.
export function useEventReading(input) {
  const text=ref(''),complete=ref(false),visible=ref(false),step=ref('inactive'),controlledFrame=ref(null)
  const diagnostic=ref(null),fullText=ref(''),language=ref('ja')
  let active=null,key='',timer=null,generation=0,imageReady=false,blocked=false
  const reduced=()=>matchMedia('(prefers-reduced-motion: reduce)').matches
  const writer=createTypewriter({publish:s=>{text.value=s.text;complete.value=s.complete}})
  function cancelWait(){++generation;clearTimeout(timer);timer=null}
  function later(ms,fn){const token=generation;timer=setTimeout(()=>{timer=null;if(token===generation)fn()},ms)}
  function type(line,phase){cancelWait();step.value=phase;fullText.value=line;visible.value=true;writer.reset(line,{instant:reduced()||Boolean(active?.dialogue.diagnostic),paused:blocked})}
  function startLine(){
    if(!active||blocked||!imageReady||step.value!=='waiting-image')return
    if(active.nightDeath&&!reduced()&&!diagnostic.value){step.value='waiting-death';later(2000,()=>type(active.dialogue.text,'primary'))}
    else type(active.dialogue.text,'primary')
  }
  function ready(){if(!active||imageReady)return;imageReady=true;cancelWait();startLine()}
  function imageFailed(){if(!active)return;diagnostic.value='DIALOGUE_IMAGE_UNAVAILABLE';imageReady=true;cancelWait();if(step.value==='waiting-image'||step.value==='waiting-death')type(active.dialogue.text,'primary')}
  function advanceLocal(){
    if(!active||blocked||!visible.value||step.value==='execution-transition')return
    if(!complete.value){writer.reveal();return}
    if(step.value==='primary'&&active.execution){
      cancelWait();writer.pause();visible.value=false;step.value='execution-transition';controlledFrame.value=1
      if(reduced())type(active.consequence,'consequence')
      else later(2000,()=>type(active.consequence,'consequence'))
    }
  }
  watch(input,value=>{
    const nextKey=value.enabled&&value.current ? value.key : ''
    const line=nextKey?eventDialogue(value.current.event):null
    const identity=nextKey+'\0'+(line?.text||'')
    if(identity!==key){
      language.value=line?.diagnostic?'zh-CN':'ja'
      key=identity;cancelWait();writer.dispose();active=null;imageReady=false;visible.value=false;complete.value=false;text.value='';fullText.value='';diagnostic.value=null;controlledFrame.value=null;step.value='inactive'
      if(nextKey){
        const valid=!line.diagnostic&&!value.artwork?.diagnostic
        active={dialogue:line,execution:valid&&value.current.stage==='EXECUTION'&&Boolean(value.artwork?.consequence),consequence:value.artwork?.consequence||'',nightDeath:valid&&value.current.stage==='NIGHT_RESULT'&&value.current.event?.eventname!=='wsw'&&value.artwork?.frames?.length===2}
        controlledFrame.value=active.execution?0:null;step.value='waiting-image';fullText.value=line.text
        imageReady=value.artwork?.mode==='deferred'
        if(!imageReady)later(15000,imageFailed)
      }
    }
    const wasBlocked=blocked;blocked=Boolean(value.blocked)
    if(blocked){writer.pause();cancelWait();return}
    if(wasBlocked){
      if(visible.value)writer.play()
      else if(step.value==='waiting-death')later(2000,()=>type(active.dialogue.text,'primary'))
      else if(step.value==='execution-transition')later(2000,()=>type(active.consequence,'consequence'))
      else if(step.value==='waiting-image'&&!imageReady)later(15000,imageFailed)
    }
    startLine()
  },{immediate:true})
  onBeforeUnmount(()=>{cancelWait();writer.dispose()})
  const canConfirm=computed(()=>visible.value&&complete.value&&(!active?.execution||step.value==='consequence')&&!blocked)
  const localAction=computed(()=>!canConfirm.value&&visible.value&&!blocked)
  return {text,fullText,language,complete,visible,step,controlledFrame,diagnostic,canConfirm,localAction,ready,imageFailed,advanceLocal}
}
