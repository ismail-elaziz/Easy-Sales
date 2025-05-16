package com.example.projetandroid.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.projetandroid.adapters.SoldProductAdapter;
import com.example.projetandroid.databinding.ActivityInvoiceDetailBinding;
import com.example.projetandroid.model.Invoice;
import com.example.projetandroid.model.SoldProduct;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceDetailActivity extends AppCompatActivity {

    private ActivityInvoiceDetailBinding binding;
    private String invoiceId;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private SoldProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Obtenir l'ID de la facture depuis l'intent
        invoiceId = getIntent().getStringExtra("INVOICE_ID");
        if (invoiceId == null) {
            Toast.makeText(this, "ID de facture manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurer le recyclerview pour les produits
        binding.recyclerViewInvoiceProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SoldProductAdapter(new ArrayList<>());
        binding.recyclerViewInvoiceProducts.setAdapter(adapter);

        // Configurer les boutons
        binding.btnEditInvoice.setOnClickListener(v -> editInvoice());
        binding.btnDeleteInvoice.setOnClickListener(v -> confirmDeleteInvoice());

        // Charger les données de la facture
        loadInvoiceDetails();
    }

    private void loadInvoiceDetails() {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference invoiceRef = mDatabase.child("users").child(userId).child("invoices").child(invoiceId);

        invoiceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Invoice invoice = snapshot.getValue(Invoice.class);
                if (invoice != null) {
                    displayInvoiceDetails(invoice);
                } else {
                    Toast.makeText(InvoiceDetailActivity.this, "Facture introuvable", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InvoiceDetailActivity.this, "Erreur: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayInvoiceDetails(Invoice invoice) {
        // Afficher les informations principales de la facture
        binding.tvClientName.setText(invoice.getClientName());
        
        // Formater et afficher la date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(invoice.getTimestamp()));
        binding.tvInvoiceDate.setText(formattedDate);
        
        // Afficher le total
        binding.tvTotal.setText(String.format(Locale.getDefault(), "%.2f €", invoice.getTotalAmount()));
        
        // Mettre à jour la liste des produits
        if (invoice.getSoldProducts() != null) {
            List<SoldProduct> products = new ArrayList<>(invoice.getSoldProducts().values());
            adapter.updateProducts(products);
        }
    }

    private void editInvoice() {
        // Rediriger vers l'activité de vente avec les informations de la facture pour modification
        Intent intent = new Intent(this, InvoiceSaleActivity.class);
        intent.putExtra("INVOICE_ID", invoiceId);
        intent.putExtra("EDIT_MODE", true);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteInvoice() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la facture")
                .setMessage("Êtes-vous sûr de vouloir supprimer cette facture ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> deleteInvoice())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteInvoice() {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference invoiceRef = mDatabase.child("users").child(userId).child("invoices").child(invoiceId);

        invoiceRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Facture supprimée avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}