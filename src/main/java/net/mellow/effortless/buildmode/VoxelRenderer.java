package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;

public class VoxelRenderer {
    
    public static void renderBlocks(List<BlockPos> blocks, EntityPlayer player, Operation operation, float partialTicks) {
        Tessellator tess = Tessellator.instance;
        
        double dx = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double dy = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(1F, 1F, 1F);
        GL11.glLineWidth(2.0F);
        GL11.glDepthMask(false);
        
        tess.setTranslation(-dx, -dy, -dz);
        tess.startDrawing(GL11.GL_LINES);
        tess.setBrightness(240);
        
        switch (operation) {
            case PLACE: tess.setColorRGBA_F(0.5F, 1.0F, 1.0F, 1.0F); break;
            case BREAK: tess.setColorRGBA_F(1.0F, 0.0F, 0.0F, 1.0F); break;
        }
        
        
        // edge detection is a fucky thing
        Set<BlockPos> set = new HashSet<>(blocks);
        
        for (BlockPos pos : blocks) {
            boolean px = set.contains(pos.add(1, 0, 0));
            boolean nx = set.contains(pos.add(-1, 0, 0));
            boolean py = set.contains(pos.add(0, 1, 0));
            boolean ny = set.contains(pos.add(0, -1, 0));
            boolean pz = set.contains(pos.add(0, 0, 1));
            boolean nz = set.contains(pos.add(0, 0, -1));
            
            double minX = pos.x;
            double maxX = pos.x + 1;
            double minY = pos.y;
            double maxY = pos.y + 1;
            double minZ = pos.z;
            double maxZ = pos.z + 1;
            
            if (!py) {
                if (!nx) {
                    tess.addVertex(minX, maxY, minZ);
                    tess.addVertex(minX, maxY, maxZ);
                }
                
                if (!pz) {
                    tess.addVertex(minX, maxY, maxZ);
                    tess.addVertex(maxX, maxY, maxZ);
                }
                
                if (!px) {
                    tess.addVertex(maxX, maxY, maxZ);
                    tess.addVertex(maxX, maxY, minZ);
                }
                
                if (!nz) {
                    tess.addVertex(maxX, maxY, minZ);
                    tess.addVertex(minX, maxY, minZ);
                }
            }
            
            if (!ny) {
                if (!nx) {
                    tess.addVertex(minX, minY, minZ);
                    tess.addVertex(minX, minY, maxZ);
                }
                
                if (!pz) {
                    tess.addVertex(minX, minY, maxZ);
                    tess.addVertex(maxX, minY, maxZ);
                }
                
                if (!px) {
                    tess.addVertex(maxX, minY, maxZ);
                    tess.addVertex(maxX, minY, minZ);
                }
                
                if (!nz) {
                    tess.addVertex(maxX, minY, minZ);
                    tess.addVertex(minX, minY, minZ);
                }
            }
            
            if (!nz) {
                if (!nx) {
                    tess.addVertex(minX, minY, minZ);
                    tess.addVertex(minX, maxY, minZ);
                }
                
                if (!py) {
                    tess.addVertex(minX, maxY, minZ);
                    tess.addVertex(maxX, maxY, minZ);
                }
                
                if (!px) {
                    tess.addVertex(maxX, maxY, minZ);
                    tess.addVertex(maxX, minY, minZ);
                }
                
                if (!ny) {
                    tess.addVertex(maxX, minY, minZ);
                    tess.addVertex(minX, minY, minZ);
                }
            }
            
            if (!pz) {
                if (!nx) {
                    tess.addVertex(minX, minY, maxZ);
                    tess.addVertex(minX, maxY, maxZ);
                }
                
                if (!py) {
                    tess.addVertex(minX, maxY, maxZ);
                    tess.addVertex(maxX, maxY, maxZ);
                }
                
                if (!px) {
                    tess.addVertex(maxX, maxY, maxZ);
                    tess.addVertex(maxX, minY, maxZ);
                }
                
                if (!ny) {
                    tess.addVertex(maxX, minY, maxZ);
                    tess.addVertex(minX, minY, maxZ);
                }
            }
            
            if (!nx) {
                if (!nz) {
                    tess.addVertex(minX, minY, minZ);
                    tess.addVertex(minX, maxY, minZ);
                }
                
                if (!py) {
                    tess.addVertex(minX, maxY, minZ);
                    tess.addVertex(minX, maxY, maxZ);
                }
                
                if (!pz) {
                    tess.addVertex(minX, maxY, maxZ);
                    tess.addVertex(minX, minY, maxZ);
                }
                
                if (!ny) {
                    tess.addVertex(minX, minY, maxZ);
                    tess.addVertex(minX, minY, minZ);
                }
            }
            
            if (!px) {
                if (!nz) {
                    tess.addVertex(maxX, minY, minZ);
                    tess.addVertex(maxX, maxY, minZ);
                }
                
                if (!py) {
                    tess.addVertex(maxX, maxY, minZ);
                    tess.addVertex(maxX, maxY, maxZ);
                }
                
                if (!pz) {
                    tess.addVertex(maxX, maxY, maxZ);
                    tess.addVertex(maxX, minY, maxZ);
                }
                
                if (!ny) {
                    tess.addVertex(maxX, minY, maxZ);
                    tess.addVertex(maxX, minY, minZ);
                }
            }
        }
        
        
        tess.draw();
        tess.setTranslation(0, 0, 0);
        
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
    
    public static void renderBlock(BlockPos block, EntityPlayer player, Operation operation, float partialTicks) {
        List<BlockPos> list = new ArrayList<>();
        list.add(block);
        renderBlocks(list, player, operation, partialTicks);
    }
    
}
