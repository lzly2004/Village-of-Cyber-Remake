// Local text only. Callers decide when the already-public line is allowed to start.
export const typewriterDelay = 50
export function createTypewriter({publish, schedule=(fn,ms)=>setTimeout(fn,ms), cancel=id=>clearTimeout(id), delay=typewriterDelay}) {
  let points=[],index=0,timer=null,version=0,paused=false,disposed=false
  const notify=()=>publish({text:points.slice(0,index).join(''),complete:index===points.length})
  const stop=()=>{++version;if(timer!==null)cancel(timer);timer=null}
  function play() {
    if(disposed)return
    paused=false
    if(timer!==null||index===points.length)return
    const token=version
    timer=schedule(()=>{if(disposed||paused||token!==version)return;timer=null;index++;notify();play()},delay)
  }
  return {
    reset(text,{instant=false,paused:hold=false}={}) {stop();disposed=false;points=Array.from(text);index=instant?points.length:0;paused=hold;notify();if(!paused)play()},
    play,
    pause(){paused=true;stop()},
    reveal(){if(disposed)return;stop();index=points.length;notify()},
    dispose(){stop();disposed=true},
  }
}
