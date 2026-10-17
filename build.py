"""Build only; this script never installs or starts Minecraft/PCL."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,sys,urllib.request,zipfile,uuid
ROOT=Path(__file__).resolve().parent
def prepare(download=False):
    deps=ROOT/"dependencies";deps.mkdir(exist_ok=True)
    locks=json.loads((ROOT/"sdk-lock.json").read_text(encoding="utf-8"))
    records=json.loads((ROOT/"build-dependencies.json").read_text(encoding="utf-8"))
    for version in locks:
        records.append({"file":"fabric-loader-"+version["version"]+".jar","url":version["url"],"sha256":version["sha256"]})
    for profile in [locks[0],locks[-1]]:
        for lib in profile["libraries"]:
            group,artifact,version=lib["name"].split(":")[:3];name=artifact+"-"+version+".jar"
            records.append({"file":name,"url":lib["url"]+group.replace(".","/")+"/"+artifact+"/"+version+"/"+name,"sha256":lib["resolvedSha256"]})
    for entry in records:
        file=deps/entry["file"]
        if not file.exists() and download:
            with urllib.request.urlopen(entry["url"],timeout=60) as response:file.write_bytes(response.read(20_000_000))
        if not file.exists() or hashlib.sha256(file.read_bytes()).hexdigest()!=entry["sha256"]:
            raise SystemExit("Missing or mismatched SDK: "+file.name+" (use --download-sdk)")
    return deps
def main():
    sys.stdout.reconfigure(encoding="utf-8")
    p=argparse.ArgumentParser(description=__doc__);p.add_argument("--jdk",default=os.environ.get("JAVA_HOME"));p.add_argument("--download-sdk",action="store_true")
    a=p.parse_args()
    if not a.jdk:p.error("Provide --jdk or JAVA_HOME")
    deps=prepare(a.download_sdk);jdk=Path(a.jdk)
    # Compile against the oldest supported Loader API and its Mixin API.
    mixin=next(deps.glob("sponge-mixin-0.12*.jar"))
    cp=[deps/"fabric-loader-0.14.21.jar",mixin]+sorted(deps.glob("asm*-9.10.1.jar"))+[deps/x["file"] for x in json.loads((ROOT/"build-dependencies.json").read_text())]
    classes=ROOT/"work"/("build-"+uuid.uuid4().hex)/"classes";classes.mkdir(parents=True,exist_ok=True)
    sources=sorted((ROOT/"src/main/java").rglob("*.java"))
    command=[str(jdk/"bin/javac.exe"),"-J-Dfile.encoding=UTF-8","-J-Duser.language=en","-encoding","UTF-8","-proc:none","--release","17","-classpath",os.pathsep.join(map(str,cp)),"-d",str(classes)]+list(map(str,sources))
    result=subprocess.run(command,capture_output=True)
    if result.returncode:raise SystemExit(result.stderr.decode("utf-8",errors="replace"))
    version=json.loads((ROOT/"release.json").read_text(encoding="utf-8"))["version"]
    dist=ROOT/"dist";dist.mkdir(exist_ok=True);jar=dist/("FabricLoadingFramework-"+version+".jar")
    with zipfile.ZipFile(jar,"w",zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(classes.rglob("*.class")):archive.write(file,file.relative_to(classes).as_posix())
        for file in sorted((ROOT/"src/main/resources").rglob("*")):
            if file.is_file():archive.write(file,file.relative_to(ROOT/"src/main/resources").as_posix())
        for name in ["LICENSE","NOTICE","COMMERCIAL-USE.md","THIRD-PARTY-LICENSES/MIT-media.txt"]:archive.write(ROOT/name,"META-INF/"+name)
    print("BUILD_OK",jar.name,jar.stat().st_size,hashlib.sha256(jar.read_bytes()).hexdigest())
if __name__=="__main__":main()
