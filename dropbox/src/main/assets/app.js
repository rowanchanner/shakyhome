'use strict';
const form=document.getElementById('sendForm');
const pairing=document.getElementById('pairing');
const url=document.getElementById('url');
const feedback=document.getElementById('feedback');
const send=document.getElementById('send');
const progress=document.getElementById('progress');
let timer=null;
function message(text){feedback.textContent=text;}
async function poll(){
  try{
    const response=await fetch('/api/status',{headers:{'X-Sharky-Code':pairing.value.trim()},cache:'no-store',signal:AbortSignal.timeout(5000)});
    const state=await response.json();
    message(state.message);
    if(!response.ok){clearInterval(timer);timer=null;send.disabled=false;return;}
    progress.hidden=!state.busy&&state.progress<0;
    if(state.progress>=0)progress.value=state.progress;else progress.removeAttribute('value');
    send.disabled=state.busy;
    if(!state.busy){clearInterval(timer);timer=null;}
  }catch{
    clearInterval(timer);timer=null;send.disabled=false;
    message('Receiver is not visible right now. Check your TV: the installer may be open. Reopen Sharky DropBox to send another link.');
  }
}
form.addEventListener('submit',async event=>{
  event.preventDefault();
  let link=url.value.trim();
  const second=link.indexOf('https://',8);
  if(second>0&&link.slice(0,second)===link.slice(second)){link=link.slice(0,second);url.value=link;}
  try{const parsed=new URL(link);if(parsed.protocol!=='https:'||parsed.username||parsed.password)throw Error();}
  catch{message('Paste a full https:// download link.');return;}
  if(!/^\d{6}$/.test(pairing.value.trim())){message('Enter the six-digit code shown on your TV.');pairing.focus();return;}
  send.disabled=true;message('Sending link…');
  try{
    const response=await fetch('/api/drop',{method:'POST',headers:{'Content-Type':'application/json','X-Sharky-Code':pairing.value.trim()},body:JSON.stringify({url:link}),signal:AbortSignal.timeout(8000)});
    const state=await response.json();message(state.message);
    if(!response.ok){send.disabled=false;return;}
    if(timer)clearInterval(timer);
    timer=setInterval(poll,1000);
  }catch{send.disabled=false;message('Could not reach your Fire Stick. Open Sharky DropBox on the TV and check both devices are on the same Wi-Fi.');}
});
