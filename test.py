"""Headless source/SDK checks; never starts Minecraft, PCL or native windows."""
from pathlib import Path
import argparse,json,os,subprocess,sys,uuid
ROOT=Path(__file__).resolve().parent
def main():
    sys.stdout.reconfigure(encoding="utf-8")
    p=argparse.ArgumentParser(description=__doc__);p.add_argument("--jdk",required=True);p.add_argument("--java",action="append",default=[]);p.add_argument("--engine",action="store_true");p.add_argument("--game-sdk",type=Path);p.add_argument("--ffmpeg",type=Path)
    a=p.parse_args();deps=ROOT/"dependencies";work=ROOT/"work"/("tests-"+uuid.uuid4().hex);classes=work/"classes";classes.mkdir(parents=True,exist_ok=True)
    common=sorted(deps.glob("asm*-9.10.1.jar"))+[deps/i["file"] for i in json.loads((ROOT/"build-dependencies.json").read_text())]
    mixin=next(deps.glob("sponge-mixin-0.17*.jar"));jar=ROOT/"dist/FabricLoadingFramework-0.1.0-dev.jar"
    cp=[jar,deps/"fabric-loader-0.19.5.jar",mixin]+common
    env=dict(os.environ);tmp=work/"tmp";tmp.mkdir(exist_ok=True);env.update({k:str(tmp) for k in ["TMP","TEMP","TMPDIR"]})
    compile=[str(Path(a.jdk)/"bin/javac.exe"),"-J-Dfile.encoding=UTF-8","-J-Duser.language=en","-proc:none","-encoding","UTF-8","--release","17","-cp",os.pathsep.join(map(str,cp)),"-d",str(classes)]+list(map(str,(ROOT/"src/test/java").rglob("*.java")))
    subprocess.run(compile,check=True,env=env)
    outputs=[]
    def execute(java,paths,main,arguments,label):
        cmd=[java,"-Djava.awt.headless=true","-Dfile.encoding=UTF-8","-Djava.io.tmpdir="+str(tmp),"-cp",os.pathsep.join(map(str,paths)),main]+list(map(str,arguments))
        result=subprocess.run(cmd,capture_output=True,env=env,timeout=60)
        output=result.stdout.decode("utf-8",errors="replace")+result.stderr.decode("utf-8",errors="replace")
        print(output,end="")
        outputs.append({"label":label,"java":java,"returncode":result.returncode,"output":output})
        audit=ROOT/"audit";audit.mkdir(exist_ok=True);(audit/"headless-checks.json").write_text(json.dumps(outputs,ensure_ascii=False,indent=2),encoding="utf-8")
        if result.returncode:raise SystemExit(result.returncode)
    for index,java in enumerate(a.java or [str(Path(a.jdk)/"bin/java.exe")]):
        execute(java,[classes]+cp,"FabricRegression",[work/("case-"+str(index)),classes/"fixtures/Initializer.class",ROOT],"core")
    if a.engine:
        for loader,pattern in [("0.14.21","sponge-mixin-0.12*.jar"),("0.19.5","sponge-mixin-0.17*.jar")]:
            paths=[classes,jar,deps/("fabric-loader-"+loader+".jar"),next(deps.glob(pattern))]+common
            execute(str(Path(a.jdk)/"bin/java.exe"),paths,"MixinEngineSmoke",[classes,work/("engine-"+loader)],"actual Mixin "+loader)
    if a.game_sdk:
        execute(str(Path(a.jdk)/"bin/java.exe"),[classes]+cp,"GameAbiSmoke",[a.game_sdk],"vanilla SDK ABI")
    if a.ffmpeg:
        import shutil
        media=work/"media";(media/"bin").mkdir(parents=True);(media/"video").mkdir()
        shutil.copy2(a.ffmpeg,media/"bin/ffmpeg.exe")
        subprocess.run([str(a.ffmpeg),"-nostdin","-hide_banner","-loglevel","error","-f","lavfi","-i","testsrc2=size=320x180:rate=30","-t","1","-c:v","libx264","-pix_fmt","yuv420p",str(media/"video/loading.mp4")],check=True,env=env,timeout=30)
        execute(str(Path(a.jdk)/"bin/java.exe"),[classes]+cp,"DecoderSmoke",[media],"actual FFmpeg decode")
    print("TEST_WORK",work)
if __name__=="__main__":main()
