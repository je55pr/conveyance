// Forge maps these stable SRG names in both development and installed games.
function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var IntInsnNode = Java.type('org.objectweb.asm.tree.IntInsnNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    function clear(method) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables !== null) method.localVariables.clear();
        method.maxStack = 2;
        method.maxLocals = 1;
    }
    function maximum(owner, name) {
        return {
            target: {type: 'METHOD', 'class': owner, methodName: ASMAPI.mapMethod(name), methodDesc: '()I'},
            transformer: function(method) {
                clear(method);
                method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 100));
                method.instructions.add(new InsnNode(Opcodes.IRETURN));
                ASMAPI.log('INFO', 'Conveyance: 100-item stack limit applied to ' + owner);
                return method;
            }
        };
    }
    return {
        item_maximum: maximum('net.minecraft.world.item.Item', 'm_41459_'),
        stack_maximum: maximum('net.minecraft.world.item.ItemStack', 'm_41741_'),
        container_maximum: maximum('net.minecraft.world.Container', 'm_6893_'),
        damaged_items_stack: {
            target: {type: 'METHOD', 'class': 'net.minecraft.world.item.ItemStack', methodName: ASMAPI.mapMethod('m_41753_'), methodDesc: '()Z'},
            transformer: function(method) {
                clear(method);
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, 'net/minecraft/world/item/ItemStack', ASMAPI.mapMethod('m_41619_'), '()Z', false));
                method.instructions.add(new InsnNode(Opcodes.ICONST_1));
                method.instructions.add(new InsnNode(Opcodes.IXOR));
                method.instructions.add(new InsnNode(Opcodes.IRETURN));
                return method;
            }
        }
    };
}
