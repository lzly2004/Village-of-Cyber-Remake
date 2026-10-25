// One non-looping effect voice, bounded current-scene queue. No decoded PCM bank.
export const SOUND_GAIN = 0.25
export function createSoundChannel({resources,getContext,allowed,createMedia,urls,notify,timeoutMs}) {
  let media, source, gain, url, request, timer, version=0, queue=[], active=null, disposed=false, volume=1
  const emit=(status='idle',error=null)=>notify({status,id:active,error})
  function clear() {
    ++version;clearTimeout(timer);request?.abort();request=null
    if(media){media.onended=media.onerror=null;media.pause();media.removeAttribute('src');media.load()}
    if(url){urls.revokeObjectURL(url);url=null}
    active=null
  }
  function reset({preserveClick=false}={}) {
    queue=[]
    // CLICK belongs to the user's interaction, not the old presentation cursor.
    if(preserveClick && active==='CLICK')return
    clear();emit()
  }
  function disconnect() {
    source?.disconnect();gain?.disconnect();media=source=gain=null
  }
  function fail(token,code) {if(token===version&&!disposed){queue=[];clear();emit('error',code)}}
  async function next() {
    clear()
    if(disposed || !allowed() || !queue.length){queue=[];emit();return}
    const token=version,id=queue.shift();active=id;emit('loading')
    timer=setTimeout(()=>fail(token,'SOUND_TIMEOUT'),timeoutMs)
    try {
      const {asset,data}=await resources.load('sounds',id,{signal:(request=new AbortController()).signal})
      if(token!==version||disposed||!allowed())return
      if(!media){
        try {
          media=createMedia();media.preload='none';media.loop=false
          const context=getContext();source=context.createMediaElementSource(media);gain=context.createGain()
          gain.gain.value=SOUND_GAIN*volume;source.connect(gain);gain.connect(context.destination)
        }catch(error){if(media){media.pause();media.removeAttribute('src');media.load()}disconnect();throw error}
      }
      url=urls.createObjectURL(new Blob([data],{type:asset.mime}));media.src=url
      media.onerror=()=>fail(token,'SOUND_MEDIA_FAILED')
      media.onended=()=>{if(token===version)void next()}
      await media.play()
      if(token!==version||disposed||!allowed())return
      if(getContext().state!=='running')throw Error('device interrupted')
      clearTimeout(timer)
      timer=setTimeout(()=>fail(token,'SOUND_END_TIMEOUT'),(asset.duration+5)*1000)
      emit('playing')
    }catch(error){fail(token,error?.name==='NotAllowedError'?'SOUND_GESTURE_REQUIRED':
      /^[A-Z][A-Z0-9_]*$/.test(error?.code)?error.code:'SOUND_PLAY_FAILED')}
  }
  return {
    enqueue(ids,{click=false}={}) {
      if(disposed||!allowed())return
      // Scene sounds have priority; rapid clicks never build a backlog.
      if(click&&(active||queue.length))return
      // Keep the three-item cap: a full scene queue may preempt feedback CLICK.
      if(!click && Array.isArray(ids) && active==='CLICK' && ids.length+queue.length+1>3)clear()
      if(!Array.isArray(ids)||ids.length+queue.length+(active?1:0)>3){reset();emit('error','SOUND_QUEUE_INVALID');return}
      queue.push(...ids);if(!active)void next()
    },
    reset,
    level(value){volume=value;if(gain)gain.gain.value=SOUND_GAIN*volume;if(!allowed())reset()},
    dispose(){if(disposed)return;reset();disconnect();disposed=true},
  }
}
