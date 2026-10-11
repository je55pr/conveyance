"""Create static OBJ casings, per-block collision bounds and separate drill meshes."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def add(a, b): return tuple(x + y for x, y in zip(a, b))
def sub(a, b): return tuple(x - y for x, y in zip(a, b))
def mul(a, s): return tuple(x * s for x in a)
def dot(a, b): return sum(x * y for x, y in zip(a, b))
def cross(a, b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def normal(points):
    n = cross(sub(points[1], points[0]), sub(points[2], points[0]))
    length = math.sqrt(dot(n, n))
    return mul(n, 1 / length) if length > 1e-9 else (0, 0, 0)


class Mesh:
    def __init__(self):
        self.faces = []
        self.solids = []

    def face(self, points, material):
        n = normal(points)
        if dot(n, n) < 0.5: return
        uv = ((0, 0), (1, 0), (1, 1), (0, 1))
        self.faces.append((material, n, [tuple(p) + uv[i] for i, p in enumerate(points)]))

    def box(self, low, high, material):
        x, y, z = low; X, Y, Z = high
        self.solids.append(list(low) + list(high))
        for points in (
            [(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)],
            [(X,y,Z),(x,y,Z),(x,Y,Z),(X,Y,Z)],
            [(x,y,Z),(x,y,z),(x,Y,z),(x,Y,Z)],
            [(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],
            [(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],
            [(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)]):
            # The first order points into the box; reverse for outward faces.
            self.face(list(reversed(points)), material)

    def cylinder(self, x, z, y0, y1, r0, r1, material, sides=8, cap=True):
        bottom = [(x + r0 * math.cos(i*2*math.pi/sides), y0, z + r0*math.sin(i*2*math.pi/sides)) for i in range(sides)]
        top = [(x + r1 * math.cos(i*2*math.pi/sides), y1, z + r1*math.sin(i*2*math.pi/sides)) for i in range(sides)]
        for i in range(sides):
            j = (i + 1) % sides
            self.face([bottom[i], top[i], top[j], bottom[j]], material)
            if cap:
                self.face([(x,y1,z),top[j],top[i]], material)
                self.face([(x,y0,z),bottom[i],bottom[j]], material)
        self.solids.append([x-max(r0,r1),y0,z-max(r0,r1),x+max(r0,r1),y1,z+max(r0,r1)])

    def taper(self, x, z, y0, y1, width0, width1, material):
        a, b = width0/2, width1/2
        bottom = [(x-a,y0,z-a),(x+a,y0,z-a),(x+a,y0,z+a),(x-a,y0,z+a)]
        top = [(x-b,y1,z-b),(x+b,y1,z-b),(x+b,y1,z+b),(x-b,y1,z+b)]
        for i in range(4):
            j = (i+1)%4
            self.face([bottom[i],top[i],top[j],bottom[j]],material)
        self.face(list(reversed(top)),material)
        self.face(bottom,material)
        for i in range(6):
            w = width0 + (width1-width0)*i/6
            self.solids.append([x-w/2,y0+(y1-y0)*i/6,z-w/2,x+w/2,y0+(y1-y0)*(i+1)/6,z+w/2])

    def extend(self, other, offset=(0,0,0)):
        for material, n, vertices in other.faces:
            self.faces.append((material,n,[add(v[:3],offset)+v[3:] for v in vertices]))
        self.solids.extend([list(add(s[:3],offset))+list(add(s[3:],offset)) for s in other.solids])


def materials(accent="yellow_concrete", casing="light_gray_concrete"):
    return {"metal":"minecraft:block/iron_block", "dark":"minecraft:block/polished_deepslate",
            "case":f"minecraft:block/{casing}", "accent":f"minecraft:block/{accent}",
            "status":"minecraft:block/black_concrete", "stone":"minecraft:block/stone_bricks",
            "pane":"minecraft:block/blue_concrete", "pane2":"minecraft:block/cyan_concrete"}


def drill(height, radius):
    mesh = Mesh()
    mesh.cylinder(.5,.5,.25,height,.10,.10,"metal")
    mesh.cylinder(.5,.5,.035,.36,0,radius*.85,"dark")
    # A broad helical flange makes rotation readable from every side.
    for i in range(28):
        vertices = []
        for t, r in ((i/28,.12),(i/28,radius),((i+1)/28,radius),((i+1)/28,.12)):
            a = t * math.pi * 4
            vertices.append((.5 + math.cos(a)*r, .32 + t*(height-.38), .5 + math.sin(a)*r))
        mesh.face(vertices,"metal")
        mesh.face(list(reversed(vertices)),"metal")
    return mesh


def miner(width, height):
    mesh = Mesh(); c = width/2
    mesh.box((.12,.04,.12),(width-.12,.20,width-.12),"dark")
    for x in (.33,width-.33):
        for z in (.33,width-.33):
            mesh.box((x-.19,.2,z-.19),(x+.19,.33,z+.19),"metal")
            mesh.box((x-.075,.33,z-.075),(x+.075,height-.50,z+.075),"metal")
    y = height-.60
    mesh.box((.28,y,.28),(width-.28,y+.15,.47),"accent")
    mesh.box((.28,y,width-.47),(width-.28,y+.15,width-.28),"accent")
    mesh.box((.28,y,.47),(.47,y+.15,width-.47),"metal")
    mesh.box((width-.47,y,.47),(width-.28,y+.15,width-.47),"metal")
    mesh.box((c-.55,y-.10,c-.45),(c+.55,height-.22,c+.45),"case")
    mesh.box((c-.27,height-.22,c-.26),(c+.27,height-.10,c+.26),"dark")
    mesh.box((.20,.22,.12),(.80,.38,.43),"metal")
    if width == 2:
        mesh.box((.61,.25,1.47),(1.39,.92,1.84),"dark")
        mesh.box((.66,.92,1.52),(1.34,1.04,1.79),"accent")
    else:
        for x in (.82,1.02,1.98,2.18):
            mesh.box((x,y+.01,.31),(x+.05,height-.29,.41),"dark")
    mesh.solids.append([c-.38,0,c-.38,c+.38,height-.6,c+.38])
    return mesh


def furnace():
    mesh = Mesh()
    mesh.box((.12,.02,.12),(1.88,.19,1.88),"dark")
    mesh.taper(1,1,.19,1.62,1.64,1.04,"stone")
    mesh.taper(1,1,1.62,1.83,1.04,.57,"stone")
    mesh.cylinder(1,1,1.80,2.77,.29,.23,"stone",cap=False)
    mesh.cylinder(1,1,2.76,2.92,.34,.34,"dark",cap=False)
    mesh.cylinder(1,1,2.75,2.77,.21,.21,"dark")
    mesh.box((.52,.36,.13),(1.48,1.0,.27),"dark")
    mesh.box((.66,.48,.115),(1.34,.87,.14),"status")
    for x in (.78,.97,1.16): mesh.box((x,.48,.10),(x+.035,.87,.115),"metal")
    return mesh


def lab():
    mesh = Mesh()
    mesh.box((.12,.03,.12),(2.88,.24,2.88),"dark")
    mesh.cylinder(1.5,1.5,.24,.37,1.26,1.26,"metal",sides=12)
    rings = []
    for ring in range(3):
        theta = ring * math.pi / 6
        rings.append([(1.5+1.22*math.cos(theta)*math.cos((i+ring*.5)*math.pi/6),
                       .37+1.49*math.sin(theta),
                       1.5+1.22*math.cos(theta)*math.sin((i+ring*.5)*math.pi/6)) for i in range(12)])
    facets = []
    for ring in range(2):
        for i in range(12):
            j = (i+1)%12
            facets.extend(([rings[ring][i],rings[ring+1][i],rings[ring][j]],
                           [rings[ring][j],rings[ring+1][i],rings[ring+1][j]]))
    for i in range(12): facets.append([rings[2][i],(1.5,1.86,1.5),rings[2][(i+1)%12]])
    for index, points in enumerate(facets):
        n = normal(points)
        center = mul(tuple(sum(p[a] for p in points) for a in range(3)),1/3)
        if dot(n,sub(center,(1.5,.37,1.5))) < 0: points = list(reversed(points)); n = normal(points)
        mesh.face(points,"pane2" if index%4 == 0 else "pane")
        for i in range(3):
            a,b = points[i],points[(i+1)%3]
            inset_a = add(a,mul(sub(center,a),.085)); inset_b = add(b,mul(sub(center,b),.085))
            band = [add(p,mul(n,.004)) for p in (a,b,inset_b,inset_a)]
            if dot(normal(band),n)<0: band.reverse()
            mesh.face(band,"metal")
    # Conservative, rounded collision slices, rather than invisible full cubes.
    for iy in range(8):
        radius = 1.22*math.sqrt(1-(iy/8)**2)
        for ix in range(10):
            a = -radius+2*radius*ix/10; b = -radius+2*radius*(ix+1)/10
            nearest = 0 if a<=0<=b else min(abs(a),abs(b))
            half_z = math.sqrt(max(0,radius*radius-nearest*nearest))
            mesh.solids.append([1.5+a,.37+1.49*iy/8,1.5-half_z,1.5+b,.37+1.49*(iy+1)/8,1.5+half_z])
    mesh.box((1.19,.28,.13),(1.81,.64,.30),"metal")
    mesh.box((1.27,.38,.115),(1.73,.54,.13),"status")
    return mesh


def assembler():
    mesh = Mesh()
    mesh.box((.12,.03,.12),(2.88,.22,2.88),"dark")
    mesh.box((.29,.22,.29),(2.71,1.25,2.71),"case")
    mesh.taper(1.5,1.5,1.25,1.62,2.42,2.08,"metal")
    mesh.box((.39,.55,.26),(2.61,.69,.29),"accent")
    mesh.box((.66,.69,.18),(1.27,1.04,.29),"dark")
    mesh.box((.73,.77,.165),(1.2,.95,.18),"status")
    mesh.box((1.0,1.62,1.04),(2.0,1.84,1.96),"case")
    for x in (1.12,1.31,1.50,1.69,1.88): mesh.box((x,1.84,1.15),(x+.045,1.89,1.85),"dark")
    for x in (.35,2.54):
        for z in (.35,2.54): mesh.box((x,.22,z),(x+.11,1.24,z+.11),"metal")
    return mesh


def generator():
    mesh = Mesh()
    mesh.box((.1,.02,.1),(.9,.16,.9),"dark")
    mesh.box((.17,.16,.22),(.83,.67,.78),"case")
    mesh.box((.27,.67,.34),(.73,.81,.70),"metal")
    mesh.box((.20,.29,.12),(.65,.59,.23),"dark")
    mesh.box((.27,.36,.10),(.58,.53,.12),"status")
    mesh.cylinder(.74,.73,.57,.94,.075,.075,"dark")
    for z in (.30,.40,.50,.60): mesh.box((.83,.28,z),(.87,.61,z+.035),"metal")
    return mesh


def solar():
    mesh = Mesh()
    for x in (.28,2.58):
        for z in (.28,2.58): mesh.box((x,0,z),(x+.14,.28+.18*z,z+.14),"metal")
    for row in range(3):
        for col in range(3):
            x = .17+col*.88; z = .17+row*.88; X=x+.84; Z=z+.84
            y=lambda p:.25+.18*p
            mesh.face([(x,y(z),z),(x,y(Z),Z),(X,y(Z),Z),(X,y(z),z)],"pane")
            mesh.solids.append([x,0,z,X,y(Z)+.035,Z])
            for a,b in (((x,y(z),z),(X,y(z),z)),((x,y(Z),Z),(X,y(Z),Z)),
                        ((x,y(z),z),(x,y(Z),Z)),((X,y(z),z),(X,y(Z),Z))):
                low = [min(a[i],b[i])-.02 for i in range(3)]; high=[max(a[i],b[i])+.02 for i in range(3)]
                mesh.box(low,high,"metal")
    mesh.box((.3,.05,.12),(.67,.20,.21),"dark")
    return mesh


def horizontal_cylinder(mesh, x, y, z0, z1, radius, material):
    cylinder = Mesh()
    cylinder.cylinder(0, 0, z0, z1, radius, radius, material, sides=12)
    for mat, _, vertices in cylinder.faces:
        mesh.face([(x + v[0], y + v[2], v[1]) for v in reversed(vertices)], mat)
    mesh.solids.append([x-radius, y-radius, z0, x+radius, y+radius, z1])


def boiler():
    mesh = Mesh()
    mesh.box((.12,0,.12),(2.88,.16,1.88),"dark")
    mesh.box((.30,.16,.28),(2.70,1.10,1.72),"stone")
    mesh.box((.42,1.10,.39),(2.58,1.32,1.61),"case")
    mesh.box((1.12,.35,1.72),(1.88,.83,1.76),"dark")
    mesh.box((1.24,.44,1.76),(1.76,.72,1.79),"status")
    mesh.cylinder(2.33,1.36,1.16,1.90,.18,.14,"dark")
    mesh.cylinder(2.33,1.36,1.85,1.96,.20,.20,"metal")
    # Blue water ports at opposite ends of the back row, orange steam at the front.
    mesh.box((0,.36,1.36),(.31,.64,1.64),"pane")
    mesh.box((2.69,.36,1.36),(3,.64,1.64),"pane")
    mesh.box((1.36,.36,0),(1.64,.64,.30),"accent")
    for x in (.47,.91,1.35,1.79,2.23): mesh.box((x,1.32,.54),(x+.055,1.38,1.47),"metal")
    return mesh


def steam_engine():
    mesh = Mesh()
    mesh.box((.12,0,.12),(2.88,.13,4.88),"dark")
    for x in (.32,2.48): mesh.box((x,.13,.33),(x+.20,.28,4.67),"metal")
    horizontal_cylinder(mesh,1.5,.58,.72,3.10,.35,"case")
    for z in (.88,2.77):
        mesh.box((.92,.22,z),(2.08,.36,z+.19),"dark")
        mesh.box((1.10,.27,z+.04),(1.90,.92,z+.14),"metal")
    mesh.box((1.40,.47,3.10),(1.60,.67,4.26),"metal")
    horizontal_cylinder(mesh,.65,.54,3.54,3.88,.42,"dark")
    horizontal_cylinder(mesh,.65,.54,3.49,3.94,.12,"accent")
    mesh.box((.31,.49,3.47),(.99,.59,3.50),"metal")
    mesh.box((.60,.20,3.47),(.70,.88,3.50),"metal")
    mesh.box((1.87,.29,3.28),(2.65,.73,4.10),"case")
    mesh.box((2.65,.38,3.40),(2.69,.63,3.96),"status")
    mesh.box((1.35,.35,0),(1.65,.65,.73),"accent")
    mesh.box((1.35,.35,4.25),(1.65,.65,5),"accent")
    return mesh


def offshore_pump():
    mesh = Mesh()
    mesh.box((.12,0,.10),(.88,.15,1.45),"dark")
    mesh.cylinder(.5,.48,.15,1.65,.29,.25,"case")
    mesh.cylinder(.5,.48,1.63,1.78,.28,.28,"metal")
    mesh.box((.33,.36,.78),(.67,1.67,1.05),"metal")
    mesh.box((.35,1.35,1.05),(.65,1.65,2),"pane")
    mesh.box((.18,1.07,.69),(.22,1.34,.89),"status")
    return mesh


def pump_intake():
    mesh = Mesh()
    mesh.box((.36,.36,-.67),(.64,.64,.21),"metal")
    mesh.box((.36,-.82,-.67),(.64,.36,-.39),"metal")
    mesh.box((.29,-.94,-.74),(.71,-.80,-.32),"dark")
    return mesh


def pumpjack():
    mesh = Mesh()
    mesh.box((.12,0,.12),(2.88,.15,2.88),"dark")
    for x in (.35,2.35): mesh.box((x,.15,.35),(x+.25,1.65,2.30),"case")
    mesh.box((.30,1.65,.25),(2.70,1.87,2.30),"accent")
    mesh.box((1.35,.30,.48),(1.65,1.80,.78),"metal")
    mesh.cylinder(1.50,1.50,.18,.60,.48,.48,"dark")
    mesh.box((.70,.50,1.10),(2.30,.86,2.50),"case")
    mesh.box((2.36,.36,0),(2.64,.64,.45),"metal")
    mesh.box((.32,.60,2.40),(.70,.90,2.44),"status")
    return mesh


def refinery():
    mesh = Mesh()
    mesh.box((.12,0,.12),(4.88,.16,4.88),"dark")
    for x,z,r in ((1.1,1.4,.65),(2.6,1.5,.65),(3.8,2.7,.65),(1.2,3.6,.72)):
        mesh.cylinder(x,z,.16,1.60,r,r,"case",sides=12)
        mesh.cylinder(x,z,1.60,1.85,r,.18,"metal",sides=12)
    mesh.box((2.0,.18,3.20),(3.20,1.35,4.20),"case")
    mesh.box((2.08,.53,4.20),(2.58,.91,4.23),"status")
    # Water/crude inputs behind, heavy/light/petroleum outputs in front.
    for x,mat in ((1.5,"pane"),(3.5,"dark")): mesh.box((x-.15,.35,4.1),(x+.15,.65,5),mat)
    for x,mat in ((1.5,"accent"),(2.5,"metal"),(3.5,"pane2")): mesh.box((x-.15,.35,0),(x+.15,.65,.8),mat)
    for z in (2.55,2.95): mesh.box((.6,1.07,z),(3.6,1.23,z+.12),"metal")
    return mesh


def chemical_plant():
    mesh = Mesh()
    mesh.box((.12,0,.12),(2.88,.16,2.88),"dark")
    for x in (.85,2.05):
        mesh.cylinder(x,1.2,.16,1.55,.48,.48,"case",sides=12)
        mesh.cylinder(x,1.2,1.55,1.85,.48,.12,"metal",sides=12)
    mesh.box((.75,.4,2.08),(2.25,1.24,2.70),"case")
    mesh.box((1.06,.61,2.70),(1.73,.95,2.74),"status")
    for x,mat in ((.5,"pane2"),(2.5,"pane")): mesh.box((x-.15,.35,2.05),(x+.15,.65,3),mat)
    for x in (.5,2.5): mesh.box((x-.15,.35,0),(x+.15,.65,.73),"metal")
    mesh.box((.8,1.1,1.6),(2.15,1.24,1.78),"metal")
    return mesh


def clip_polygon(vertices, axis, bound, above):
    result=[]
    if not vertices: return result
    for a,b in zip(vertices,vertices[1:]+vertices[:1]):
        inside_a = a[axis]>=bound-1e-9 if above else a[axis]<=bound+1e-9
        inside_b = b[axis]>=bound-1e-9 if above else b[axis]<=bound+1e-9
        if inside_a: result.append(a)
        if inside_a != inside_b:
            t=(bound-a[axis])/(b[axis]-a[axis])
            result.append(tuple(a[i]+(b[i]-a[i])*t for i in range(5)))
    return result


def part_mesh(mesh, x,y,z):
    result=Mesh(); origin=(x,y,z)
    for material,n,vertices in mesh.faces:
        low=[min(v[a] for v in vertices) for a in range(3)]; high=[max(v[a] for v in vertices) for a in range(3)]
        if any(high[a]<origin[a]-1e-8 or low[a]>origin[a]+1+1e-8 for a in range(3)): continue
        if any(abs(high[a]-low[a])<1e-8 and math.floor(low[a]-n[a]*1e-7)!=origin[a] for a in range(3)): continue
        for axis in range(3):
            vertices=clip_polygon(vertices,axis,origin[axis],True)
            vertices=clip_polygon(vertices,axis,origin[axis]+1,False)
        if len(vertices)<3: continue
        vertices=[sub(v[:3],origin)+v[3:] for v in vertices]
        for i in range(1,len(vertices)-1):
            triangle=[vertices[0],vertices[i],vertices[i+1]]
            if dot(normal([v[:3] for v in triangle]),n)>.5: result.faces.append((material,n,triangle))
    for solid in mesh.solids:
        lo=[max(solid[a],origin[a])-origin[a] for a in range(3)]
        hi=[min(solid[a+3],origin[a]+1)-origin[a] for a in range(3)]
        if all(hi[a]-lo[a]>1e-7 for a in range(3)): result.solids.append([round(v,6) for v in lo+hi])
    return result


def write_json(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8")


def write_obj(path,mesh):
    lines=["# Generated Conveyance machine geometry", "mtllib ../machine_materials.mtl"]
    index=1; current=None
    for material,n,vertices in mesh.faces:
        if current!=material: lines.append("usemtl "+material); current=material
        polygons=[vertices] if len(vertices)<=4 else [[vertices[0],vertices[i],vertices[i+1]] for i in range(1,len(vertices)-1)]
        for polygon in polygons:
            for v in polygon: lines.append("v "+" ".join(f"{p:.6f}" for p in v[:3]))
            for v in polygon: lines.append(f"vt {v[3]:.6f} {v[4]:.6f}")
            for v in polygon: lines.append("vn "+" ".join(f"{p:.6f}" for p in n))
            lines.append("f "+" ".join(f"{index+i}/{index+i}/{index+i}" for i in range(len(polygon))))
            index+=len(polygon)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text("\n".join(lines)+"\n",encoding="utf-8")


def model(root,name,mesh,textures):
    folder=root/"assets/conveyance/models/block/machines"
    write_obj(folder/(name+".obj"),mesh)
    write_json(folder/(name+".json"),{"parent":"minecraft:block/block","loader":"forge:obj",
        "model":"conveyance:models/block/machines/"+name+".obj", "mtl_override":"conveyance:models/block/machines/"+("creative_materials.mtl" if name.startswith("generator/") else "machine_materials.mtl"),
        "automatic_culling":False,"flip_v":True,"emissive_ambient":False,"textures":textures|{"particle":"minecraft:block/magenta_concrete" if name.startswith("generator/") else textures["case"]}})


def definitions():
    return {"miner":(3,3,miner(3,3),materials()),
            "burner_miner":(2,2,miner(2,2),materials("orange_concrete","copper_block")),
            "industrial_furnace":(2,3,furnace(),materials()),
            "research_lab":(3,2,lab(),materials("light_blue_concrete")),
            "assembler":(3,2,assembler(),materials("copper_block")),
            "generator":(1,1,generator(),materials("magenta_concrete","iron_block")),
            "solar_panel":(3,1,solar(),materials()),
            "boiler":(3,2,boiler(),materials("orange_concrete")),
            "steam_engine":(3,1,steam_engine(),materials("orange_concrete","gray_concrete")),
            "offshore_pump":(1,2,offshore_pump(),materials("blue_concrete")),
            "pumpjack":(3,2,pumpjack(),materials("yellow_concrete")),
            "oil_refinery":(5,2,refinery(),materials("orange_concrete")),
            "chemical_plant":(3,2,chemical_plant(),materials("cyan_concrete"))}


def generate_all(root=ROOT):
    folder=root/"assets/conveyance/models/block/machines"; folder.mkdir(parents=True,exist_ok=True)
    folder.joinpath("machine_materials.mtl").write_text("\n".join(
        f"newmtl {name}\nKd 1 1 1\nKa 0 0 0\nmap_Kd #{name}\n" for name in materials()),encoding="utf-8")
    folder.joinpath("creative_materials.mtl").write_text("\n".join(
        f"newmtl {name}\nKd {'1 0.12 0.82' if name in ('metal','case') else '1 1 1'}\nKa 0 0 0\nmap_Kd #{name}\n" for name in materials()),encoding="utf-8")
    write_json(folder/"empty.json",{"textures":{"particle":"minecraft:block/iron_block"},"elements":[]})
    shapes={}
    for name,(width,height,mesh,textures) in definitions().items():
        depth={"boiler":2,"steam_engine":5,"offshore_pump":2}.get(name,width)
        count=width*depth*height; parts=[]
        lit_textures=textures|{"status":"minecraft:block/orange_concrete" if name in ("industrial_furnace","generator") else "minecraft:block/lime_concrete"}
        if name=="research_lab": lit_textures|={"pane":"minecraft:block/light_blue_concrete","pane2":"minecraft:block/cyan_concrete"}
        for part in range(count):
            clipped=part_mesh(mesh,part%width,part//(width*depth),part//width%depth)
            # Intake is visual only and extends below the bank into the water.
            if name=="offshore_pump" and part==0: clipped.faces.extend(pump_intake().faces)
            parts.append(clipped.solids)
            model(root,f"{name}/part_{part}",clipped,textures)
            if name not in ("miner","burner_miner"):
                write_json(folder/f"{name}/part_{part}_on.json",{"parent":f"conveyance:block/machines/{name}/part_{part}","textures":lit_textures})
        shapes[name]=parts
        state_count=48 if name=="miner" else 50 if name!="generator" else 1
        variants={}
        for part in range(state_count):
            for facing,rotation in FACINGS if name!="generator" else (("north",0),):
                for lit in (False,True) if name!="miner" else (False,):
                    key=(f"part={part},facing={facing}" if name!="generator" else "")+(f",lit={str(lit).lower()}" if name not in ("miner","generator") else f"lit={str(lit).lower()}" if name=="generator" else "")
                    path=f"conveyance:block/machines/{name}/part_{part}"+("_on" if lit and name not in ("miner","burner_miner") else "") if part<count else "conveyance:block/machines/empty"
                    variants[key]={"model":path,"y":rotation}
        write_json(root/f"assets/conveyance/blockstates/{name}.json",{"variants":variants})
        if name!="generator": write_json(root/f"assets/conveyance/blockstates/{name}_part.json",{"variants":variants})
        full=Mesh(); full.extend(mesh)
        if name=="offshore_pump": full.extend(pump_intake())
        if name in ("miner","burner_miner"):
            rotating=drill(height-.65,.34 if width==3 else .27)
            model(root,f"{name}/drill",rotating,textures)
            full.extend(rotating,(width/2-.5,0,width/2-.5))
        item=Mesh(); scale=max(width,depth,height)
        for material,n,vertices in full.faces:
            item.faces.append((material,n,[((v[0]-width/2)/scale+.5,v[1]/scale,(v[2]-depth/2)/scale+.5)+v[3:] for v in vertices]))
        model(root,f"{name}/item",item,textures)
        sprite = root/f"assets/conveyance/textures/item/{name}.png"
        write_json(root/f"assets/conveyance/models/item/{name}.json", {"parent":"minecraft:item/generated", "textures":{"layer0":f"conveyance:item/{name}"}}
                   if sprite.exists() and name != "generator" else {"parent":f"conveyance:block/machines/{name}/item"})
    write_json(root/"assets/conveyance/machine_shapes.json",shapes)


def preview(path):
    from PIL import Image, ImageDraw, ImageFont
    image=Image.new("RGB",(1280,1040),(24,31,38)); draw=ImageDraw.Draw(image)
    font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",18)
    palette={"metal":(190,201,209),"dark":(48,55,60),"case":(126,141,151),"accent":(224,177,63),
             "stone":(143,133,117),"status":(230,116,32),"pane":(56,136,202),"pane2":(48,104,162)}
    def scene(mesh,center,scale):
        polygons=[]
        for material,n,vertices in mesh.faces:
            if dot(n,(1,.8,-1))<=0: continue
            color=palette[material]; brightness=.5+.5*max(0,dot(n,(.35,.8,-.48)))
            points=[(center[0]+(v[0]+v[2])*.72*scale,center[1]+((v[0]-v[2])*.38-v[1])*scale) for v in vertices]
            depth=sum(v[0]-v[2]+v[1]*.76 for v in vertices)/len(vertices)
            polygons.append((depth,points,tuple(int(c*brightness) for c in color)))
        for _,points,color in sorted(polygons,key=lambda p:p[0]): draw.polygon(points,fill=color)
    labels={"miner":"Electric miner · 3×3×3","burner_miner":"Burner miner · 2×2×2","industrial_furnace":"Stone furnace · 2×2×3",
            "research_lab":"Research lab · 3×3×2","assembler":"Assembler · 3×3×2","generator":"Burner generator · 1×1×1","solar_panel":"Solar panel · 3×3×1"}
    for i,(name,(width,height,mesh,_)) in enumerate(definitions().items()):
        left=i%4*320; top=i//4*325
        if name not in labels: continue
        draw.text((left+14,top+12),labels[name],font=font,fill=(227,235,240))
        full=Mesh(); full.extend(mesh)
        if name in ("miner","burner_miner"): full.extend(drill(height-.65,.34 if width==3 else .27),(width/2-.5,0,width/2-.5))
        scale=76 if width>1 else 155
        scene(full,(left+160-width*.72*scale,top+270),scale)
    draw.text((22,674),"Three furnaces on adjacent 2×2 footprints — casing gaps and separate chimneys",font=font,fill=(227,235,240))
    row=Mesh()
    for i in range(3): row.extend(furnace(),(i*2,0,0))
    scene(row,(335,975),94)
    draw.text((22,1010),"Geometry preview; Minecraft applies its block textures in game.",font=font,fill=(151,170,184))
    path.parent.mkdir(parents=True,exist_ok=True); image.save(path)


if __name__=="__main__":
    generate_all()
    import sys
    if "--preview" in sys.argv: preview(ROOT.parents[2]/".tools/machine-models-preview.png")

# Keep expanded early-game assets consistent after regenerating base assets.
import runpy
runpy.run_path(str(Path(__file__).with_name("generate_early_game_assets.py")))
