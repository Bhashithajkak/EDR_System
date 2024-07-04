package com.example.edrsystem.dynamic_analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
public class ExtractPacketFeatures {
    public void runPacketMonitor() {
    }
    public void runPacketMonitor(int interfaceNumber) {
        try {
            // Tshark command to capture traffic with specific fields
            String tsharkCommand = "tshark -i " + interfaceNumber +
                    " -T fields -e frame.time_epoch -e ip.src -e ip.dst -e tcp.srcport -e tcp.dstport " +
                    "-e udp.srcport -e udp.dstport -e frame.len -e _ws.col.Protocol";

            // Use ProcessBuilder to execute the Tshark command
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", tsharkCommand);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<PacketFeatures> packetFeaturesList = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                PacketFeatures features = extractFeatures(line);
                if (features != null) {
                    packetFeaturesList.add(features);
                }
                //System.out.println(line);  // Print the output for debugging
            }

            // Process the collected features
            processPacketFeatures(packetFeaturesList);

            // Wait for the process to finish
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private PacketFeatures extractFeatures(String line) {
        String[] fields = line.split("\t");
        if (fields.length < 8) {
            return null;  // Skip lines that don't have all required fields
        }

        PacketFeatures features = new PacketFeatures();
        try {
            features.time = Double.parseDouble(fields[0]);
            features.srcIp = fields[1];
            features.dstIp = fields[2];
            features.srcPort = Integer.parseInt(fields[3].isEmpty() ? fields[5] : fields[3]);  // Use UDP port if TCP is empty
            features.dstPort = Integer.parseInt(fields[4].isEmpty() ? fields[6] : fields[4]);  // Use UDP port if TCP is empty
            features.packetSize = Integer.parseInt(fields[7]);
            features.protocol = fields[8];
        } catch (NumberFormatException e) {
            return null;  // Skip lines with parsing errors
        }
        System.out.println(features.packetSize);
        return features;

    }

    private void processPacketFeatures(List<PacketFeatures> packetFeaturesList) {
        // This could involve passing the data to a machine learning model,
        System.out.println("Processing " + packetFeaturesList.size() + " packets...");

        // Example: Calculate average packet size
        double avgPacketSize = packetFeaturesList.stream()
                .mapToInt(p -> p.packetSize)
                .average()
                .orElse(0.0);

        System.out.println("Average packet size: " + avgPacketSize);

        // Add more analysis as needed for your EDR system
    }




    private static class PacketFeatures {
        double time;
        String srcIp;
        String dstIp;
        int srcPort;
        int dstPort;
        int packetSize;
        String protocol;
    }
}