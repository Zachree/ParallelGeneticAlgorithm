package com.company;

import javax.swing.*;
import java.awt.*;

public class GUI {
    private JPanel panel = new JPanel();
    int generations = 0;
    JLabel label1 = new JLabel("a");
    JLabel label2 = new JLabel("b");
    JLabel label3 = new JLabel("c");
    JLabel label4 = new JLabel("d");
    JLabel label5 = new JLabel("e");
    JLabel label6 = new JLabel("f");
    JLabel label7 = new JLabel("g");
    JLabel label8 = new JLabel("h");
    JLabel label9 = new JLabel("u");
    JLabel label10 = new JLabel("j");
    JLabel label11 = new JLabel("k");
    JLabel label12 = new JLabel("l");
    JLabel label13 = new JLabel("m");
    JLabel label14 = new JLabel("n");
    JLabel label15 = new JLabel("o");
    JLabel label16 = new JLabel("p");
    JLabel label17 = new JLabel("q");
    JLabel label18 = new JLabel("r");
    JLabel label19 = new JLabel("s");
    JLabel label20 = new JLabel("0");

    public GUI() {
        JFrame f = new JFrame();
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 400, 100));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel.add(label1);
        panel.add(label2);
        panel.add(label3);
        panel.add(label4);
        panel.add(label5);
        panel.add(label6);
        panel.add(label7);
        panel.add(label8);
        panel.add(label9);
        panel.add(label10);
        panel.add(label11);
        panel.add(label12);
        panel.add(label13);
        panel.add(label14);
        panel.add(label15);
        panel.add(label16);
        panel.add(label17);
        panel.add(label18);
        panel.add(label19);
        panel.add(label20);
        f.add(panel, BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);
    }

    public void updateLabel(int[][] room, int population, int isolated, double fitness) {
        String[] roomString = new String[room[0].length];
        String temp = "";
        for(int i = 0; i < 16; i++){
            for(int j = 0; j < 16; j++) {
                temp = temp + room[i][j] + " ";
            }
            roomString[i] = temp + "\n";
            temp = "";
        }
        label1.setText(roomString[0]);
        label2.setText(roomString[1]);
        label3.setText(roomString[2]);
        label4.setText(roomString[3]);
        label5.setText(roomString[4]);
        label6.setText(roomString[5]);
        label7.setText(roomString[6]);
        label8.setText(roomString[7]);
        label9.setText(roomString[8]);
        label10.setText(roomString[9]);
        label11.setText(roomString[10]);
        label12.setText(roomString[11]);
        label13.setText(roomString[12]);
        label14.setText(roomString[13]);
        label15.setText(roomString[14]);
        label16.setText(roomString[15]);
        label17.setText("Population: " + population);
        label18.setText("Isolated: " + isolated);
        label19.setText("Fitness Score: " + fitness);
        generations++;
        label20.setText("Generations: " + generations + " (max 100)");
    }
}
